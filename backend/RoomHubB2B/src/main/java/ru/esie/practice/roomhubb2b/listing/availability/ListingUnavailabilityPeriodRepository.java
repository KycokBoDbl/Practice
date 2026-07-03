package ru.esie.practice.roomhubb2b.listing.availability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ListingUnavailabilityPeriodRepository
        extends JpaRepository<ListingUnavailabilityPeriodEntity, Long> {

    @Query("""
            SELECT period
            FROM ListingUnavailabilityPeriodEntity period
            WHERE period.listingId = :listingId
              AND period.startAt < :to
              AND period.endAt > :from
            ORDER BY period.startAt, period.endAt
            """)
    List<ListingUnavailabilityPeriodEntity> findOverlapping(
            @Param("listingId") Long listingId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT CASE WHEN COUNT(period) > 0 THEN true ELSE false END
            FROM ListingUnavailabilityPeriodEntity period
            WHERE period.listingId = :listingId
              AND period.startAt < :endAt
              AND period.endAt > :startAt
            """)
    boolean existsOverlap(
            @Param("listingId") Long listingId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Modifying
    @Query("DELETE FROM ListingUnavailabilityPeriodEntity period WHERE period.booking.id = :bookingId")
    void deleteByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT COUNT(period) FROM ListingUnavailabilityPeriodEntity period WHERE period.booking.id = :bookingId")
    long countByBookingId(@Param("bookingId") Long bookingId);
}
