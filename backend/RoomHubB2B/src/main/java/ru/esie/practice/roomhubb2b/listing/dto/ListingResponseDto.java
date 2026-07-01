package ru.esie.practice.roomhubb2b.listing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.esie.practice.roomhubb2b.listing.SpaceType;

import java.math.BigDecimal;

@Schema(description = "Published commercial space listing")
public record ListingResponseDto(
        @Schema(description = "Listing identifier", example = "42") Long id,
        @Schema(description = "Listing title", example = "Meeting room in the city center") String title,
        @Schema(description = "City where the space is located", example = "Barnaul") String city,
        @Schema(description = "Rental price per hour", example = "2500.00") BigDecimal pricePerHour,
        @Schema(description = "Maximum number of guests", example = "20") Integer capacity,
        @Schema(description = "Commercial space type", example = "MEETING_ROOM") SpaceType spaceType,
        @Schema(description = "Public listing image URL", example = "https://example.com/listing-42.jpg") String imageUrl,
        @Schema(description = "Detailed listing description") String description,
        @Schema(description = "Street address", example = "Lenina Avenue, 10") String address
) {
}
