from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="orchestra-j-sidecar")


class SidecarRequest(BaseModel):
    sessionId: str
    goalId: str
    instructionJson: str


class SidecarResponse(BaseModel):
    success: bool
    confidence: float
    payload: str
    error: str | None = None


@app.post("/inference/run-task", response_model=SidecarResponse)
def run_task(request: SidecarRequest) -> SidecarResponse:
    return SidecarResponse(
        success=True,
        confidence=0.91,
        payload=request.instructionJson,
        error=None,
    )
