package ru.esie.practice.roomhubb2b.listing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ListingRepository extends JpaRepository<ListingEntity, Long> {

    List<ListingEntity> findByStatus(String status);
}