package ru.esie.practice.roomhubb2b.listing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Published commercial space listing")
public class ListingResponseDto {

    @Schema(description = "Listing identifier", example = "42")
    private Long id;
    @Schema(description = "Listing title", example = "Meeting room in the city center")
    private String title;
    @Schema(description = "City where the space is located", example = "Barnaul")
    private String city;
    @Schema(description = "Rental price per hour", example = "2500.00")
    private BigDecimal pricePerHour;
    @Schema(description = "Maximum number of guests", example = "20")
    private Integer capacity;
    @Schema(description = "Commercial space type", example = "MEETING_ROOM")
    private String spaceType;
    @Schema(description = "Public listing image URL", example = "https://example.com/listing-42.jpg")
    private String imageUrl;
    @Schema(description = "Detailed listing description")
    private String description;
    @Schema(description = "Street address", example = "Lenina Avenue, 10")
    private String address;

    public ListingResponseDto(
            Long id,
            String title,
            String description,
            String city,
            String address,
            BigDecimal pricePerHour,
            Integer capacity,
            String spaceType,
            String imageUrl
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.city = city;
        this.address = address;
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

    public String getDescription() {
        return description;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
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
