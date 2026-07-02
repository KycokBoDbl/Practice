package ru.esie.practice.roomhubb2b.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void findsUserUsingNormalizedEmail() {
        OrganizationEntity organization = organizationRepository.save(
                new OrganizationEntity("ООО Repository Test", "7200000001")
        );
        userRepository.saveAndFlush(new UserEntity(
                organization,
                "owner@example.com",
                "owner@example.com",
                passwordEncoder.encode("S3cure-roomhub-password"),
                UserRole.LANDLORD
        ));

        assertThat(userRepository.findByEmailNormalized(AuthService.normalizeEmail(" OWNER@EXAMPLE.COM ")))
                .isPresent()
                .get()
                .extracting(UserEntity::getRole)
                .isEqualTo(UserRole.LANDLORD);
        assertThat(organizationRepository.findByTaxId("7200000001"))
                .isPresent()
                .get()
                .extracting(OrganizationEntity::getLegalName)
                .isEqualTo("ООО Repository Test");
    }
}
