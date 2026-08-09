package org.brian.aisupportagent.dto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidLoginRequest() {
        LoginRequest request = new LoginRequest("employee@example.com", "password");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankEmailAndPassword() {
        LoginRequest request = new LoginRequest(" ", "");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertTrue(violations.size() >= 2);
        assertTrue(hasViolationFor(violations, "email"));
        assertTrue(hasViolationFor(violations, "password"));
    }

    private boolean hasViolationFor(
            Set<ConstraintViolation<LoginRequest>> violations,
            String fieldName
    ) {
        return violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(fieldName));
    }
}
