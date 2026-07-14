package ru.esie.practice.roomhubb2b.listing.ai;

import ru.esie.practice.roomhubb2b.listing.SpaceType;

import java.math.BigDecimal;

public record AiListingSearchFilter(
        String city,
        SpaceType spaceType,
        Integer minCapacity,
        BigDecimal maxPricePerHour,
        int limit
) {
}
