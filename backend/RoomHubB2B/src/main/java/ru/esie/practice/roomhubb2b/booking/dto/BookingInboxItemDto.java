package ru.esie.practice.roomhubb2b.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.esie.practice.roomhubb2b.booking.BookingEntity;
import ru.esie.practice.roomhubb2b.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Schema(description = "Booking summary item for the participant inbox")
public record BookingInboxItemDto(
        @Schema(description = "Booking identifier", example = "81") Long id,
        @Schema(description = "Listing identifier", example = "42") Long listingId,
        @Schema(description = "Listing title", example = "Conference room on Lenin Ave") String listingTitle,
        @Schema(description = "Current booking status", example = "REQUESTED") BookingStatus status,
        @Schema(type = "string", example = "2026-07-10T10:00") String startAt,
        @Schema(type = "string", example = "2026-07-10T13:00") String endAt,
        @Schema(description = "Hourly price captured at booking time", example = "2500.00") BigDecimal pricePerHour,
        @Schema(description = "Total captured booking price", example = "7500.00") BigDecimal totalPrice,
        @Schema(type = "string", nullable = true, example = "2026-07-10T10:30")
        String confirmationDeadline,
        @Schema(description = "Tenant organization legal name", example = "Demo Tenant LLC")
        String tenantOrganizationName,
        @Schema(description = "Landlord organization legal name", example = "Demo Landlord LLC")
        String landlordOrganizationName,
        @Schema(type = "string", example = "2026-07-07T12:40")
        String createdAt,
        @Schema(type = "string", example = "2026-07-07T12:40")
        String updatedAt
) {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm");

    public static BookingInboxItemDto from(BookingEntity booking) {
        return new BookingInboxItemDto(
                booking.getId(),
                booking.getListing().getId(),
                booking.getListing().getTitle(),
                booking.getStatus(),
                format(booking.getStartAt()),
                format(booking.getEndAt()),
                booking.getPricePerHour(),
                booking.getTotalPrice(),
                format(booking.getConfirmationDeadline()),
                organizationName(booking.getTenantOrganization()),
                organizationName(booking.getListing().getOwnerOrganization()),
                format(booking.getCreatedAt()),
                format(booking.getUpdatedAt())
        );
    }

    private static String format(LocalDateTime value) {
        return value == null ? null : FORMATTER.format(value);
    }

    private static String organizationName(ru.esie.practice.roomhubb2b.auth.OrganizationEntity organization) {
        return organization == null ? null : organization.getLegalName();
    }
}
