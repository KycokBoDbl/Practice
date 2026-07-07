package ru.esie.practice.roomhubb2b.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import ru.esie.practice.roomhubb2b.auth.dto.RegisterRequestDto;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsLegalEntityRegistrationData() {
        RegisterRequestDto request = request("2225123456", "S3cure-roomhub-password");

        assertThat(validator.validate(request)).isEmpty();
        assertThat(validator.validate(request("2225123456", "12345678"))).isEmpty();
    }

    @Test
    void rejectsTwelveDigitTaxId() {
        RegisterRequestDto request = request("222512345678", "S3cure-roomhub-password");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("taxId"));
    }

    @Test
    void rejectsPasswordOutsideBcryptLimits() {
        assertThat(validator.validate(request("2225123456", "1234567")))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
        assertThat(validator.validate(request("2225123456", "я".repeat(37))))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
        assertThat(validator.validate(request("2225123456", "a".repeat(65))))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    @Test
    void rejectsMissingRequiredFields() {
        RegisterRequestDto request = new RegisterRequestDto(null, " ", null, " ", null);

        Set<String> invalidFields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).contains("role", "legalName", "taxId", "email", "password");
    }

    private static RegisterRequestDto request(String taxId, String password) {
        return new RegisterRequestDto(
                UserRole.LANDLORD,
                "ООО Деловой центр",
                taxId,
                "owner@example.com",
                password
        );
    }
}
