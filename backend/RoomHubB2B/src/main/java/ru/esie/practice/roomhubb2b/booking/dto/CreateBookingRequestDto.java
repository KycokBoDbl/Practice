package ru.esie.practice.roomhubb2b.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateBookingRequestDto(
        @NotNull
        @Schema(description = "Published listing identifier", example = "42")
        Long listingId,
        @NotNull
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):00$")
        @Schema(type = "string", example = "2026-07-10T10:00")
        String startAt,
        @NotNull
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):00$")
        @Schema(type = "string", example = "2026-07-10T13:00")
        String endAt
) {
}
