package ru.esie.practice.roomhubb2b.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponseDto(
        @Schema(description = "Signed access token") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "900") long expiresIn
) {
}
