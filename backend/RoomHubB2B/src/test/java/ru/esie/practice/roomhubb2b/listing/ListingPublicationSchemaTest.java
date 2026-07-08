package ru.esie.practice.roomhubb2b.listing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ListingPublicationSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @ValueSource(strings = {"PUBLISHED", "ARCHIVED"})
    void acceptsSupportedListingStatuses(String status) {
        Long id = jdbcTemplate.queryForObject(
                """
                        INSERT INTO listings
                            (title, city, address, price_per_hour, capacity, space_type, status)
                        VALUES ('Supported status listing', 'Barnaul', 'Lenina Avenue, 10',
                                2500.00, 20, 'MEETING_ROOM', ?)
                        RETURNING id
                        """,
                Long.class,
                status
        );

        String storedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM listings WHERE id = ?",
                String.class,
                id
        );
        assertThat(storedStatus).isEqualTo(status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "DELETED"})
    void rejectsUnsupportedListingStatuses(String status) {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                        INSERT INTO listings
                            (title, city, address, price_per_hour, capacity, space_type, status)
                        VALUES ('Unsupported status listing', 'Barnaul', 'Lenina Avenue, 10',
                                2500.00, 20, 'MEETING_ROOM', ?)
                        """,
                status
        ));
    }

    @Test
    void blocksDeletingListingWithBookingReferences() {
        Long organizationId = jdbcTemplate.queryForObject(
                "INSERT INTO organizations (legal_name, tax_id) VALUES ('Schema tenant', '7900000201') RETURNING id",
                Long.class
        );
        Long listingId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO listings
                            (title, city, address, price_per_hour, capacity, space_type, status)
                        VALUES ('Referenced listing', 'Barnaul', 'Lenina Avenue, 10',
                                2500.00, 20, 'MEETING_ROOM', 'PUBLISHED')
                        RETURNING id
                        """,
                Long.class
        );
        jdbcTemplate.update(
                """
                        INSERT INTO bookings
                            (listing_id, tenant_organization_id, status, start_at, end_at,
                             price_per_hour, total_price)
                        VALUES (?, ?, 'REQUESTED', ?, ?, 2500.00, 5000.00)
                        """,
                listingId,
                organizationId,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 1, 12, 0)
        );

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "DELETE FROM listings WHERE id = ?",
                listingId
        ));
    }

    @ParameterizedTest(name = "rejects {0}")
    @MethodSource("invalidListings")
    void rejectsInvalidPublicationData(
            String caseName,
            String title,
            String city,
            String address,
            BigDecimal price,
            Integer capacity
    ) {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                        INSERT INTO listings
                            (title, city, address, price_per_hour, capacity, space_type, status)
                        VALUES (?, ?, ?, ?, ?, 'MEETING_ROOM', 'PUBLISHED')
                        """,
                title,
                city,
                address,
                price,
                capacity
        ));
    }

    private static Stream<Arguments> invalidListings() {
        return Stream.of(
                Arguments.of("blank title", " ", "Barnaul", "Lenina Avenue, 10", price(), 20),
                Arguments.of("blank city", "Meeting room", " ", "Lenina Avenue, 10", price(), 20),
                Arguments.of("missing address", "Meeting room", "Barnaul", null, price(), 20),
                Arguments.of("blank address", "Meeting room", "Barnaul", " ", price(), 20),
                Arguments.of("zero price", "Meeting room", "Barnaul", "Lenina Avenue, 10", BigDecimal.ZERO, 20),
                Arguments.of("zero capacity", "Meeting room", "Barnaul", "Lenina Avenue, 10", price(), 0)
        );
    }

    private static BigDecimal price() {
        return new BigDecimal("2500.00");
    }
}
