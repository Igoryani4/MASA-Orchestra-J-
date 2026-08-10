# Orchestra-J

MVP-реализация гибридной платформы **Thick Orchestrator, Thin Executor**:
- **Spring Boot Orchestrator**: workflow, HITL, memory, валидация входа.
- **Python Sidecar**: AI-inference endpoint (заглушка для дальнейшего gRPC/LLM слоя).
- **gRPC contract**: `src/main/proto/ai_sidecar.proto`.

## Что реализовано

1. Базовая агентная модель (`Agent`, `PlannerAgent`, `ExecutorAgent`, `CriticAgent`).
2. Оркестратор `WorkflowOrchestratorService` с HITL веткой при низкой уверенности.
3. Memory слой short-term (TTL + ring buffer на 20 шагов).
4. Tool Gateway + Bean Validation для биомед-инпута.
5. API:
   - `POST /api/workflow/run`
   - `POST /api/workflow/run-async`
   - `GET /api/workflow/{taskId}`
   - `POST /api/workflow/{taskId}/cancel`
   - `GET /api/hitl/pending`
   - `GET /api/hitl/{taskId}`
   - `POST /api/hitl/approve/{taskId}`
6. Python sidecar:
   - `POST /inference/run-task`
7. Для sidecar-вызовов включены timeout/retry/backoff/circuit breaker (конфиг в `application.yml`).
8. Ошибки API возвращаются в едином формате: `code`, `message`, `details`, `traceId`.

## Быстрый старт

### 1) Запуск sidecar
```bash
cd worker
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### 2) Запуск orchestrator
```bash
mvn spring-boot:run
```

### 3) Проверка workflow
```bash
curl -X POST http://localhost:8080/api/workflow/run \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: run-g1-001" \
  -d '{
    "sessionId":"s-1",
    "goalId":"g-1",
    "objective":"Analyze target",
    "domain":"biomed",
    "instructionJson":"{\"task\":\"screen\"}"
  }'
```

### 4) Асинхронный запуск workflow
```bash
curl -X POST http://localhost:8080/api/workflow/run-async \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: async-g1-001" \
  -d '{
    "sessionId":"s-1",
    "goalId":"g-1",
    "objective":"Analyze target",
    "domain":"biomed",
    "instructionJson":"{\"task\":\"screen\"}"
  }'
```

Проверка статуса:
```bash
curl http://localhost:8080/api/workflow/<taskId>
```

Отмена задачи:
```bash
curl -X POST http://localhost:8080/api/workflow/<taskId>/cancel
```
