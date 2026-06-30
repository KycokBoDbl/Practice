package ru.esie.practice.roomhubb2b.listing.availability.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "Hourly availability for a published listing")
public class ListingAvailabilityResponseDto {

    private static final DateTimeFormatter HOUR_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm");

    @Schema(description = "Listing identifier", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private final Long listingId;

    @Schema(
            description = "Inclusive query start in listing-local time without a UTC offset",
            type = "string",
            pattern = "^\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):00$",
            example = "2026-07-01T09:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final String from;

    @Schema(
            description = "Exclusive query end in listing-local time without a UTC offset",
            type = "string",
            pattern = "^\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):00$",
            example = "2026-07-03T18:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final String to;

    @ArraySchema(
            arraySchema = @Schema(
                    description = "Sorted, non-overlapping busy intervals",
                    requiredMode = Schema.RequiredMode.REQUIRED
            ),
            schema = @Schema(implementation = BusyIntervalResponseDto.class)
    )
    private final List<BusyIntervalResponseDto> busyIntervals;

    public ListingAvailabilityResponseDto(
            Long listingId,
            LocalDateTime from,
            LocalDateTime to,
            List<BusyIntervalResponseDto> busyIntervals
    ) {
        this.listingId = listingId;
        this.from = HOUR_FORMATTER.format(from);
        this.to = HOUR_FORMATTER.format(to);
        this.busyIntervals = List.copyOf(busyIntervals);
    }

    public Long getListingId() {
        return listingId;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public List<BusyIntervalResponseDto> getBusyIntervals() {
        return busyIntervals;
    }
}
