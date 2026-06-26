package ru.esie.practice.roomhubb2b.listing.dto;

import java.math.BigDecimal;

public class ListingResponseDto {

    private Long id;
    private String title;
    private String city;
    private BigDecimal pricePerHour;
    private Integer capacity;
    private String spaceType;
    private String imageUrl;

    public ListingResponseDto(
            Long id,
            String title,
            String city,
            BigDecimal pricePerHour,
            Integer capacity,
            String spaceType,
            String imageUrl
    ) {
        this.id = id;
        this.title = title;
        this.city = city;
        this.pricePerHour = pricePerHour;
        this.capacity = capacity;
        this.spaceType = spaceType;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCity() {
        return city;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getSpaceType() {
        return spaceType;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}