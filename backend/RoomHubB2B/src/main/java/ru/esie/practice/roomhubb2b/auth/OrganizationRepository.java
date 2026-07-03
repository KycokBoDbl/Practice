package ru.esie.practice.roomhubb2b.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {

    boolean existsByTaxId(String taxId);

    Optional<OrganizationEntity> findByTaxId(String taxId);
}
