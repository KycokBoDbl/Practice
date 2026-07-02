package ru.esie.practice.roomhubb2b.listing.availability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Schema(description = "Busy interval in the listing's local time")
public record BusyIntervalResponseDto(
        @Schema(
                description = "Inclusive interval start in listing-local time without a UTC offset",
                type = "string",
                pattern = "^\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):00$",
                example = "2026-07-01T12:00",
                requiredMode = Schema.RequiredMode.REQUIRED
        ) String startAt,
        @Schema(
                description = "Exclusive interval end in listing-local time without a UTC offset",
                type = "string",
                pattern = "^\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):00$",
                example = "2026-07-01T15:00",
                requiredMode = Schema.RequiredMode.REQUIRED
        ) String endAt
) {

    private static final DateTimeFormatter HOUR_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm");

    public BusyIntervalResponseDto(LocalDateTime startAt, LocalDateTime endAt) {
        this(HOUR_FORMATTER.format(startAt), HOUR_FORMATTER.format(endAt));
    }
}
