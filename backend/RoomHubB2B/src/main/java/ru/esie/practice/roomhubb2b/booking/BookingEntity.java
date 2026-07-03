package ru.esie.practice.roomhubb2b.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.listing.ListingEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private ListingEntity listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_organization_id", nullable = false)
    private OrganizationEntity tenantOrganization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "confirmation_deadline")
    private LocalDateTime confirmationDeadline;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BookingEntity() {
    }

    public BookingEntity(
            ListingEntity listing,
            OrganizationEntity tenantOrganization,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BigDecimal pricePerHour,
            BigDecimal totalPrice,
            LocalDateTime now
    ) {
        this.listing = listing;
        this.tenantOrganization = tenantOrganization;
        this.status = BookingStatus.REQUESTED;
        this.startAt = startAt;
        this.endAt = endAt;
        this.pricePerHour = pricePerHour;
        this.totalPrice = totalPrice;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void transitionTo(BookingStatus target, LocalDateTime deadline, LocalDateTime now) {
        status = target;
        confirmationDeadline = deadline;
        updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public ListingEntity getListing() {
        return listing;
    }

    public OrganizationEntity getTenantOrganization() {
        return tenantOrganization;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public LocalDateTime getConfirmationDeadline() {
        return confirmationDeadline;
    }

    public long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
