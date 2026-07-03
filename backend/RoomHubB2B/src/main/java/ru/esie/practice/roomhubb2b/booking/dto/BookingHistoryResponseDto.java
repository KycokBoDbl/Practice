package ru.esie.practice.roomhubb2b.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.esie.practice.roomhubb2b.booking.BookingStatus;
import ru.esie.practice.roomhubb2b.booking.BookingStatusHistoryEntity;

import java.time.format.DateTimeFormatter;

@Schema(description = "Append-only booking status transition")
public record BookingHistoryResponseDto(
        @Schema(example = "101") Long id,
        @Schema(nullable = true, example = "REQUESTED") BookingStatus fromStatus,
        @Schema(example = "AWAITING_CONFIRMATION") BookingStatus toStatus,
        @Schema(example = "APPROVE") String reason,
        @Schema(type = "string", example = "2026-07-01T18:00:00") String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static BookingHistoryResponseDto from(BookingStatusHistoryEntity history) {
        return new BookingHistoryResponseDto(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getReason(),
                FORMATTER.format(history.getCreatedAt())
        );
    }
}
