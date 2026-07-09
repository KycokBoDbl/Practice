package ru.esie.practice.roomhubb2b.listing;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<ListingEntity, Long> {

    @EntityGraph(attributePaths = "ownerOrganization")
    List<ListingEntity> findByStatus(ListingStatus status);

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
