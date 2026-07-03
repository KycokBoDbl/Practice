package ru.esie.practice.roomhubb2b.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.esie.practice.roomhubb2b.auth.validation.ValidEmail;

public record LoginRequestDto(
        @NotBlank
        @ValidEmail
        @Size(max = 320)
        @Schema(example = "owner@example.com")
        String email,

        @NotBlank
        @Size(max = 64)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
        String password
) {
}
