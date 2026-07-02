package ru.esie.practice.roomhubb2b.auth;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmailNormalized(String emailNormalized);

    @EntityGraph(attributePaths = "organization")
    Optional<UserEntity> findByEmailNormalized(String emailNormalized);

    @Override
    @EntityGraph(attributePaths = "organization")
    Optional<UserEntity> findById(Long id);
}
