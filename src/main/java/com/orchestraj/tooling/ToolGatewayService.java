package com.orchestraj.tooling;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ToolGatewayService {
    private final Validator validator;

    public ToolGatewayService(Validator validator) {
        this.validator = validator;
    }

    public Mono<BiomedToolInput> validateBiomedInput(BiomedToolInput input) {
        var violations = validator.validate(input);
        if (!violations.isEmpty()) {
            return Mono.error(new ConstraintViolationException(violations));
        }
        return Mono.just(input);
    }
}
