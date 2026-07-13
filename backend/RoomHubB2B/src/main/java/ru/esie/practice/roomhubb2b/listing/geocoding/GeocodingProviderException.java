package ru.esie.practice.roomhubb2b.listing.geocoding;

public class GeocodingProviderException extends RuntimeException {

    public GeocodingProviderException(String message) {
        super(message);
    }

    public GeocodingProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
