package ru.esie.practice.roomhubb2b.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ListingEnumConstraintTest {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long listingId;

    @BeforeEach
    void setUp() {
        listingId = listingRepository.findByStatus(ListingStatus.PUBLISHED).get(0).getId();
    }

    @Test
    void rejectsUnknownSpaceType() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "UPDATE listings SET space_type = ? WHERE id = ?",
                        "UNKNOWN_SPACE_TYPE",
                        listingId
                )
        );
    }

    @Test
    void rejectsUnknownStatus() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "UPDATE listings SET status = ? WHERE id = ?",
                        "UNKNOWN_STATUS",
                        listingId
                )
        );
    }
}
