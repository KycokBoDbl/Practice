package ru.esie.practice.roomhubb2b.booking;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"listing", "listing.ownerOrganization", "tenantOrganization"})
    Optional<BookingEntity> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"listing", "listing.ownerOrganization", "tenantOrganization"})
    @Query("SELECT booking FROM BookingEntity booking WHERE booking.id = :id")
    Optional<BookingEntity> findLockedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"listing", "listing.ownerOrganization", "tenantOrganization"})
    Optional<BookingEntity> findByIdAndTenantOrganizationId(Long id, Long tenantOrganizationId);

    @EntityGraph(attributePaths = {"listing", "listing.ownerOrganization", "tenantOrganization"})
    @Query("""
            SELECT booking
            FROM BookingEntity booking
            WHERE booking.id = :id
              AND booking.listing.ownerOrganization.id = :landlordOrganizationId
            """)
    Optional<BookingEntity> findByIdAndLandlordOrganizationId(
            @Param("id") Long id,
            @Param("landlordOrganizationId") Long landlordOrganizationId
    );

    @EntityGraph(attributePaths = {"listing", "listing.ownerOrganization", "tenantOrganization"})
    @Query("""
            SELECT booking
            FROM BookingEntity booking
            WHERE booking.tenantOrganization.id = :tenantOrganizationId
            ORDER BY booking.createdAt DESC, booking.id DESC
            """)
    List<BookingEntity> findInboxByTenantOrganizationId(
            @Param("tenantOrganizationId") Long tenantOrganizationId
    );

    @EntityGraph(attributePaths = {"listing", "listing.ownerOrganization", "tenantOrganization"})
    @Query("""
            SELECT booking
            FROM BookingEntity booking
            WHERE booking.listing.ownerOrganization.id = :landlordOrganizationId
            ORDER BY booking.createdAt DESC, booking.id DESC
            """)
    List<BookingEntity> findInboxByLandlordOrganizationId(
            @Param("landlordOrganizationId") Long landlordOrganizationId
    );

    @Query("""
            SELECT booking.id
            FROM BookingEntity booking
            WHERE (booking.status = ru.esie.practice.roomhubb2b.booking.BookingStatus.AWAITING_CONFIRMATION
                    AND booking.confirmationDeadline <= :now)
               OR (booking.status = ru.esie.practice.roomhubb2b.booking.BookingStatus.CONFIRMED
                    AND booking.startAt <= :now)
               OR (booking.status = ru.esie.practice.roomhubb2b.booking.BookingStatus.IN_PROGRESS
                    AND booking.endAt <= :now)
            ORDER BY booking.id
            """)
    List<Long> findDueIds(@Param("now") LocalDateTime now, Pageable pageable);
}
