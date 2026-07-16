package ru.esie.practice.roomhubb2b.listing;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<ListingEntity, Long> {

    @EntityGraph(attributePaths = "ownerOrganization")
    List<ListingEntity> findByStatus(ListingStatus status);

    @Query("""
            SELECT DISTINCT listing.city
            FROM ListingEntity listing
            WHERE listing.status = :status
            ORDER BY listing.city
            """)
    List<String> findDistinctCitiesByStatus(@Param("status") ListingStatus status);

    @EntityGraph(attributePaths = "ownerOrganization")
    @Query("""
            SELECT listing
            FROM ListingEntity listing
            WHERE listing.status = :status
              AND (:city IS NULL OR LOWER(listing.city) = :city)
              AND (:spaceType IS NULL OR listing.spaceType = :spaceType)
              AND (:minCapacity IS NULL OR listing.capacity >= :minCapacity)
              AND (:minPricePerHour IS NULL OR listing.pricePerHour >= :minPricePerHour)
              AND (:maxPricePerHour IS NULL OR listing.pricePerHour <= :maxPricePerHour)
              AND (:availabilityRequired = false OR NOT EXISTS (
                  SELECT period.id
                  FROM ListingUnavailabilityPeriodEntity period
                  WHERE period.listingId = listing.id
                    AND period.startAt < :availableTo
                    AND period.endAt > :availableFrom
              ))
            ORDER BY listing.pricePerHour ASC, listing.id ASC
            """)
    List<ListingEntity> searchPublished(
            @Param("status") ListingStatus status,
            @Param("city") String city,
            @Param("spaceType") SpaceType spaceType,
            @Param("minCapacity") Integer minCapacity,
            @Param("minPricePerHour") BigDecimal minPricePerHour,
            @Param("maxPricePerHour") BigDecimal maxPricePerHour,
            @Param("availabilityRequired") boolean availabilityRequired,
            @Param("availableFrom") LocalDateTime availableFrom,
            @Param("availableTo") LocalDateTime availableTo,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "ownerOrganization")
    @Query("""
            SELECT listing
            FROM ListingEntity listing
            WHERE listing.ownerOrganization.id = :ownerOrganizationId
              AND listing.status IN :statuses
            ORDER BY listing.id
            """)
    List<ListingEntity> findOwnedByStatuses(
            @Param("ownerOrganizationId") Long ownerOrganizationId,
            @Param("statuses") List<ListingStatus> statuses
    );

    boolean existsByIdAndStatus(Long id, ListingStatus status);

    @EntityGraph(attributePaths = "ownerOrganization")
    Optional<ListingEntity> findByIdAndStatus(Long id, ListingStatus status);

    @EntityGraph(attributePaths = "ownerOrganization")
    @Query("""
            SELECT listing
            FROM ListingEntity listing
            WHERE listing.id = :id
              AND listing.ownerOrganization.id = :ownerOrganizationId
            """)
    Optional<ListingEntity> findByIdAndOwnerOrganizationId(
            @Param("id") Long id,
            @Param("ownerOrganizationId") Long ownerOrganizationId
    );

    @Query(value = "SELECT * FROM listings WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<ListingEntity> findLockedById(@Param("id") Long id);
}
