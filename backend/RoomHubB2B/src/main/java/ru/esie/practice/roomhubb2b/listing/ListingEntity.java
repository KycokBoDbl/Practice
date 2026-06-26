package ru.esie.practice.roomhubb2b.listing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "listings")
public class ListingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String city;

    private String address;

    @Column(name = "price_per_hour")
    private BigDecimal pricePerHour;

    private Integer capacity;

    @Column(name = "space_type")
    private String spaceType;

    @Column(name = "image_url")
    private String imageUrl;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

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

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}