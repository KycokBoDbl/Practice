package ru.esie.practice.roomhubb2b.listing.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.esie.practice.roomhubb2b.listing.SpaceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Structured listing filters interpreted from a natural-language prompt")
public record AiListingSearchInterpretedFilterDto(
        @Schema(description = "Normalized city used for filtering", example = "Москва", nullable = true)
        String city,
        @Schema(description = "Commercial space type used for filtering", example = "MEETING_ROOM", nullable = true)
        SpaceType spaceType,
        @Schema(description = "Minimum required guest capacity", example = "20", nullable = true)
        Integer minCapacity,
        @Schema(description = "Minimum rental price per hour", example = "1000.00", nullable = true)
        BigDecimal minPricePerHour,
        @Schema(description = "Maximum rental price per hour", example = "5000.00", nullable = true)
        BigDecimal maxPricePerHour,
        @Schema(description = "Start of requested availability window", example = "2026-07-15T09:00", nullable = true)
        LocalDateTime availableFrom,
        @Schema(description = "End of requested availability window", example = "2026-07-15T18:00", nullable = true)
        LocalDateTime availableTo,
        @Schema(description = "Maximum number of listings requested", example = "10")
        int limit
) {
}
