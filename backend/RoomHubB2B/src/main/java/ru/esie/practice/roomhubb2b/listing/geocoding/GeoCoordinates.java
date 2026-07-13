package ru.esie.practice.roomhubb2b.listing.geocoding;

import java.math.BigDecimal;

public record GeoCoordinates(BigDecimal latitude, BigDecimal longitude) {

    public GeoCoordinates {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("latitude and longitude are required");
        }
        if (latitude.compareTo(new BigDecimal("-90")) < 0 || latitude.compareTo(new BigDecimal("90")) > 0) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (longitude.compareTo(new BigDecimal("-180")) < 0 || longitude.compareTo(new BigDecimal("180")) > 0) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }
}
