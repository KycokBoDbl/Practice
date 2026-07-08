package ru.esie.practice.roomhubb2b.listing;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<ListingEntity, Long> {

    List<ListingEntity> findByStatus(ListingStatus status);

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
