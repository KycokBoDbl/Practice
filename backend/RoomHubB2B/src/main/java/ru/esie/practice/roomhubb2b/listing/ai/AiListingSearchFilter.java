package ru.esie.practice.roomhubb2b.listing.ai;

import ru.esie.practice.roomhubb2b.listing.SpaceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AiListingSearchFilter(
        String city,
        SpaceType spaceType,
        Integer minCapacity,
        BigDecimal minPricePerHour,
        BigDecimal maxPricePerHour,
        LocalDateTime availableFrom,
        LocalDateTime availableTo,
        int limit
) {
}
