package ru.esie.practice.roomhubb2b.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AuthSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rejectsInvalidTaxIdFormat() {
        assertThrows(DataIntegrityViolationException.class, () -> insertOrganization("invalid"));
    }

    @Test
    void rejectsDuplicateTaxId() {
        insertOrganization("7100000001");
        assertThrows(DataIntegrityViolationException.class, () -> insertOrganization("7100000001"));
    }

    @Test
    void rejectsUnknownRole() {
        long organizationId = insertOrganization("7100000002");
        assertThrows(DataIntegrityViolationException.class, () -> insertUser(
                organizationId,
                "schema-role@example.com",
                "schema-role@example.com",
                "UNKNOWN"
        ));
    }

    @Test
    void rejectsDuplicateNormalizedEmail() {
        long firstOrganization = insertOrganization("7100000003");
        long secondOrganization = insertOrganization("7100000004");
        insertUser(firstOrganization, "owner@example.com", "owner@example.com", "LANDLORD");

        assertThrows(DataIntegrityViolationException.class, () -> insertUser(
                secondOrganization,
                "owner@example.com",
                "owner@example.com",
                "TENANT"
        ));
    }

    @Test
    void rejectsUserWithoutExistingOrganization() {
        assertThrows(DataIntegrityViolationException.class, () -> insertUser(
                Long.MAX_VALUE,
                "orphan@example.com",
                "orphan@example.com",
                "TENANT"
        ));
    }

    @Test
    void rejectsSecondUserForOrganization() {
        long organizationId = insertOrganization("7100000005");
        insertUser(organizationId, "first@example.com", "first@example.com", "TENANT");

        assertThrows(DataIntegrityViolationException.class, () -> insertUser(
                organizationId,
                "second@example.com",
                "second@example.com",
                "TENANT"
        ));
    }

    private long insertOrganization(String taxId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO organizations (legal_name, tax_id) VALUES (?, ?) RETURNING id",
                Long.class,
                "ООО Schema Test",
                taxId
        );
    }

    private void insertUser(long organizationId, String email, String normalizedEmail, String role) {
        jdbcTemplate.update(
                """
                        INSERT INTO users (organization_id, email, email_normalized, password_hash, role)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                organizationId,
                email,
                normalizedEmail,
                "{bcrypt}$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu",
                role
        );
    }
}
