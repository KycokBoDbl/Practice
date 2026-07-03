package ru.esie.practice.roomhubb2b.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.esie.practice.roomhubb2b.booking.BookingEntity;
import ru.esie.practice.roomhubb2b.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Schema(description = "Booking workflow state")
public record BookingResponseDto(
        @Schema(example = "81") Long id,
        @Schema(example = "42") Long listingId,
        @Schema(example = "REQUESTED") BookingStatus status,
        @Schema(type = "string", example = "2026-07-10T10:00") String startAt,
        @Schema(type = "string", example = "2026-07-10T13:00") String endAt,
        @Schema(example = "2500.00") BigDecimal pricePerHour,
        @Schema(example = "7500.00") BigDecimal totalPrice,
        @Schema(type = "string", nullable = true, example = "2026-07-10T10:30")
        String confirmationDeadline
) {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm");

    public static BookingResponseDto from(BookingEntity booking) {
        return new BookingResponseDto(
                booking.getId(),
                booking.getListing().getId(),
                booking.getStatus(),
                format(booking.getStartAt()),
                format(booking.getEndAt()),
                booking.getPricePerHour(),
                booking.getTotalPrice(),
                format(booking.getConfirmationDeadline())
        );
    }

    private static String format(LocalDateTime value) {
        return value == null ? null : FORMATTER.format(value);
    }
}
