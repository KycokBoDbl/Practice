package ru.esie.practice.roomhubb2b.listing;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ListingPublicationSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
