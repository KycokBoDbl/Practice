package ru.esie.practice.roomhubb2b.listing.availability;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.esie.practice.roomhubb2b.listing.availability.dto.ListingAvailabilityResponseDto;

import java.time.LocalDateTime;

@RestController
@Tag(name = "Listings", description = "Published commercial space listings")
public class ListingAvailabilityController {

    private static final String JAVA_HOUR_PATTERN = "uuuu-MM-dd'T'HH:mm";
    private static final String OPENAPI_HOUR_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):00$";

    private final ListingAvailabilityService availabilityService;

    public ListingAvailabilityController(ListingAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/api/listings/{listingId}/availability")
    @Operation(
            operationId = "getListingAvailability",
            summary = "Get hourly availability for a published listing"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listing availability",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ListingAvailabilityResponseDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid hourly range"),
            @ApiResponse(responseCode = "404", description = "Published listing not found")
    })
    public ListingAvailabilityResponseDto getAvailability(
            @Parameter(description = "Listing identifier", required = true, example = "42")
            @PathVariable("listingId") Long listingId,
            @Parameter(
                    description = "Inclusive start in listing-local time without a UTC offset",
                    required = true,
                    schema = @Schema(type = "string", pattern = OPENAPI_HOUR_PATTERN),
                    example = "2026-07-01T09:00"
            )
            @RequestParam("from")
            @DateTimeFormat(pattern = JAVA_HOUR_PATTERN) LocalDateTime from,
            @Parameter(
                    description = "Exclusive end in listing-local time without a UTC offset",
                    required = true,
                    schema = @Schema(type = "string", pattern = OPENAPI_HOUR_PATTERN),
                    example = "2026-07-03T18:00"
            )
            @RequestParam("to")
            @DateTimeFormat(pattern = JAVA_HOUR_PATTERN) LocalDateTime to
    ) {
        return availabilityService.getAvailability(listingId, from, to);
    }
}
