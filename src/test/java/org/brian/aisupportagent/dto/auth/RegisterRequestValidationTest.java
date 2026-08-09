package org.brian.aisupportagent.dto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestValidationTest {

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
    void acceptsValidRegistrationRequest() {
        RegisterRequest request = new RegisterRequest(
                "Brian",
                "Albert",
                "brian@example.com",
                "secure-password"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void rejectsBlankNamesInvalidEmailAndShortPassword() {
        RegisterRequest request = new RegisterRequest(
                " ",
                "",
                "not-an-email",
                "short"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertEquals(4, violations.size());
        assertTrue(hasViolationFor(violations, "firstName"));
        assertTrue(hasViolationFor(violations, "lastName"));
        assertTrue(hasViolationFor(violations, "email"));
        assertTrue(hasViolationFor(violations, "password"));
    }

    private boolean hasViolationFor(
            Set<ConstraintViolation<RegisterRequest>> violations,
            String fieldName
    ) {
        return violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(fieldName));
    }
}
