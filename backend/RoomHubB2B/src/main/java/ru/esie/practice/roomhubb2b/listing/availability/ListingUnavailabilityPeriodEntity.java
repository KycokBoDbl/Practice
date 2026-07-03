package ru.esie.practice.roomhubb2b.listing.availability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import ru.esie.practice.roomhubb2b.booking.BookingEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "listing_unavailability_periods")
public class ListingUnavailabilityPeriodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UnavailabilitySource source;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", unique = true)
    private BookingEntity booking;

    protected ListingUnavailabilityPeriodEntity() {
    }

    public ListingUnavailabilityPeriodEntity(Long listingId, LocalDateTime startAt, LocalDateTime endAt) {
        this.listingId = listingId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.source = UnavailabilitySource.MANUAL;
    }

    public ListingUnavailabilityPeriodEntity(BookingEntity booking) {
        this.listingId = booking.getListing().getId();
        this.startAt = booking.getStartAt();
        this.endAt = booking.getEndAt();
        this.source = UnavailabilitySource.BOOKING;
        this.booking = booking;
    }

    public Long getId() {
        return id;
    }

    public Long getListingId() {
        return listingId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public UnavailabilitySource getSource() {
        return source;
    }

    public Long getBookingId() {
        return booking == null ? null : booking.getId();
    }
}
