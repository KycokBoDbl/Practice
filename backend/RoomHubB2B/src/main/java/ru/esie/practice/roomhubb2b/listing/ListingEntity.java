package ru.esie.practice.roomhubb2b.listing;

import jakarta.persistence.*;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "listings")
public class ListingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    private String description;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false, length = 100)
    private SpaceType spaceType;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ListingStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_organization_id")
    private OrganizationEntity ownerOrganization;

    protected ListingEntity() {
    }

    private ListingEntity(
            String title,
            String description,
            String city,
            String address,
            BigDecimal pricePerHour,
            Integer capacity,
            SpaceType spaceType,
            String imageUrl,
            LocalDateTime createdAt,
            OrganizationEntity ownerOrganization
    ) {
        this.title = title;
        this.description = description;
        this.city = city;
        this.address = address;
        this.pricePerHour = pricePerHour;
        this.capacity = capacity;
        this.spaceType = spaceType;
        this.imageUrl = imageUrl;
        this.status = ListingStatus.PUBLISHED;
        this.createdAt = createdAt;
        this.ownerOrganization = ownerOrganization;
    }

    public static ListingEntity published(
            String title,
            String description,
            String city,
            String address,
            BigDecimal pricePerHour,
            Integer capacity,
            SpaceType spaceType,
            String imageUrl,
            LocalDateTime createdAt,
            OrganizationEntity ownerOrganization
    ) {
        return new ListingEntity(
                title,
                description,
                city,
                address,
                pricePerHour,
                capacity,
                spaceType,
                imageUrl,
                createdAt,
                ownerOrganization
        );
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

    public SpaceType getSpaceType() {
        return spaceType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrganizationEntity getOwnerOrganization() {
        return ownerOrganization;
    }

    public void updateDetails(
            String title,
            String description,
            String city,
            String address,
            BigDecimal pricePerHour,
            Integer capacity,
            SpaceType spaceType,
            String imageUrl
    ) {
        this.title = title;
        this.description = description;
        this.city = city;
        this.address = address;
        this.pricePerHour = pricePerHour;
        this.capacity = capacity;
        this.spaceType = spaceType;
        this.imageUrl = imageUrl;
    }

    public void updateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("latitude and longitude must be set together");
        }
        if (latitude != null && isOutsideRange(latitude, "-90", "90")) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (longitude != null && isOutsideRange(longitude, "-180", "180")) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean hasSameAddress(String city, String address) {
        return Objects.equals(this.city, city) && Objects.equals(this.address, address);
    }

    public void archive() {
        this.status = ListingStatus.ARCHIVED;
    }

    public void activate() {
        this.status = ListingStatus.PUBLISHED;
    }

    public void assignOwner(OrganizationEntity ownerOrganization) {
        this.ownerOrganization = ownerOrganization;
    }

    private static boolean isOutsideRange(BigDecimal value, String min, String max) {
        return value.compareTo(new BigDecimal(min)) < 0 || value.compareTo(new BigDecimal(max)) > 0;
    }
}
