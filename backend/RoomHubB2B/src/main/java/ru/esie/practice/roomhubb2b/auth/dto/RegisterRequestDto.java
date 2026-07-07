package ru.esie.practice.roomhubb2b.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.esie.practice.roomhubb2b.auth.UserRole;
import ru.esie.practice.roomhubb2b.auth.validation.ValidPassword;
import ru.esie.practice.roomhubb2b.auth.validation.ValidEmail;

public record RegisterRequestDto(
        @NotNull
        @Schema(example = "LANDLORD")
        UserRole role,

        @NotBlank
        @Size(max = 255)
        @Schema(example = "ООО Деловой центр")
        String legalName,

        @NotBlank
        @Pattern(regexp = "^[0-9]{10}$")
        @Schema(example = "2225123456", pattern = "^[0-9]{10}$")
        String taxId,

        @NotBlank
        @ValidEmail
        @Size(max = 320)
        @Schema(example = "owner@example.com")
        String email,

        @NotBlank
        @ValidPassword
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, minLength = 8, maxLength = 64)
        String password
) {
}
