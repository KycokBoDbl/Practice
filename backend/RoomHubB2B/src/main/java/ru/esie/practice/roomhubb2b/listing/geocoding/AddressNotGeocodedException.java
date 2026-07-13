package ru.esie.practice.roomhubb2b.listing.geocoding;

public class AddressNotGeocodedException extends RuntimeException {

    public AddressNotGeocodedException(String message) {
        super(message);
    }
}
