package com.orchestraj.tooling;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import reactor.test.StepVerifier;

class ToolGatewayServiceTest {

    @Test
    void shouldFailFastOnInvalidInput() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        var service = new ToolGatewayService(validator);

        BiomedToolInput input = new BiomedToolInput();
        input.setGeneSymbol("ab");
        input.setTaxId(0);
        input.setLigands(List.of("LIGAND_TOO_LONG"));

        StepVerifier.create(service.validateBiomedInput(input))
                .expectError()
                .verify();
    }
}
