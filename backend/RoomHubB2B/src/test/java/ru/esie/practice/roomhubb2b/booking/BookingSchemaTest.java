package ru.esie.practice.roomhubb2b.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class BookingSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void keepsLegacyListingWithoutOwnerValid() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM listings WHERE owner_organization_id IS NULL",
                Long.class
        );
        assertThat(count).isPositive();
    }

    @Test
    void rejectsBookingWithoutExistingTenantOrganization() {
        long listingId = listingId();
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                bookingInsertSql(),
                listingId,
                Long.MAX_VALUE,
                "REQUESTED",
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 1, 11, 0),
                100,
                100
        ));
    }

    @Test
    void rejectsUnknownStatusAndNonHourlyPeriod() {
        long tenantId = organization("7900000101");
        long listingId = listingId();
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                bookingInsertSql(),
                listingId,
                tenantId,
                "UNKNOWN",
                LocalDateTime.of(2030, 1, 1, 10, 1),
                LocalDateTime.of(2030, 1, 1, 11, 0),
                100,
                100
        ));
    }

    @Test
    void rejectsDuplicateTenantListingStartDate() {
        long tenantId = organization("7900000102");
        long listingId = listingId();
        jdbcTemplate.update(
                bookingInsertSql(),
                listingId,
                tenantId,
                "REQUESTED",
                LocalDateTime.of(2030, 1, 2, 10, 0),
                LocalDateTime.of(2030, 1, 2, 11, 0),
                100,
                100
        );

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                bookingInsertSql(),
                listingId,
                tenantId,
                "REQUESTED",
                LocalDateTime.of(2030, 1, 2, 12, 0),
                LocalDateTime.of(2030, 1, 2, 13, 0),
                100,
                100
        ));
    }

    @Test
    void enforcesCalendarSourceAndBookingReferenceConsistency() {
        long listingId = listingId();
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                        INSERT INTO listing_unavailability_periods
                            (listing_id, start_at, end_at, source, booking_id)
                        VALUES (?, ?, ?, 'BOOKING', NULL)
                        """,
                listingId,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 1, 11, 0)
        ));
    }

    private long listingId() {
        return jdbcTemplate.queryForObject("SELECT min(id) FROM listings", Long.class);
    }

    private long organization(String taxId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO organizations (legal_name, tax_id) VALUES ('Schema tenant', ?) RETURNING id",
                Long.class,
                taxId
        );
    }

    private String bookingInsertSql() {
        return """
                INSERT INTO bookings
                    (listing_id, tenant_organization_id, status, start_at, end_at,
                     price_per_hour, total_price)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
    }
}
