package ru.esie.practice.roomhubb2b.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.esie.practice.roomhubb2b.auth.UserRole;

public record ProfileResponseDto(
        @Schema(example = "12") Long userId,
        @Schema(example = "7") Long organizationId,
        @Schema(example = "LANDLORD") UserRole role,
        @Schema(example = "ООО Деловой центр") String legalName,
        @Schema(example = "2225123456") String taxId,
        @Schema(example = "owner@example.com") String email
) {
}
