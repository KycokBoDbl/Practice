package ru.esie.practice.roomhubb2b.listing.geocoding;

public interface GeocodingService {

    GeoCoordinates geocode(String city, String address);
}
