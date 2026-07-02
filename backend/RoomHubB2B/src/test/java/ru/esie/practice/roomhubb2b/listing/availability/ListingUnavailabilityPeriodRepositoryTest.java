package ru.esie.practice.roomhubb2b.listing.availability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import ru.esie.practice.roomhubb2b.listing.ListingRepository;
import ru.esie.practice.roomhubb2b.listing.ListingStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ListingUnavailabilityPeriodRepositoryTest {

    @Autowired
    private ListingUnavailabilityPeriodRepository periodRepository;

    @Autowired
    private ListingRepository listingRepository;

    private Long listingId;

    @BeforeEach
    void setUp() {
        listingId = listingRepository.findByStatus(ListingStatus.PUBLISHED).get(0).getId();
    }

    @Test
    void returnsOnlyIntervalsWithNonEmptyHalfOpenOverlap() {
        LocalDateTime queryFrom = LocalDateTime.of(2026, 7, 2, 12, 0);
        LocalDateTime queryTo = LocalDateTime.of(2026, 7, 2, 13, 0);
        periodRepository.saveAllAndFlush(List.of(
                period(queryFrom.minusHours(2), queryFrom),
                period(queryTo, queryTo.plusHours(2)),
                period(queryFrom.minusHours(1), queryFrom.plusHours(1)),
                period(queryTo.minusHours(1), queryTo.plusHours(1))
        ));

        List<ListingUnavailabilityPeriodEntity> result =
                periodRepository.findOverlapping(listingId, queryFrom, queryTo);

        assertEquals(2, result.size());
        assertEquals(queryFrom.minusHours(1), result.get(0).getStartAt());
        assertEquals(queryTo.minusHours(1), result.get(1).getStartAt());
    }

    @Test
    void rejectsIntervalsThatAreNotAlignedToWholeHours() {
        ListingUnavailabilityPeriodEntity invalid = period(
                LocalDateTime.of(2026, 7, 2, 10, 1),
                LocalDateTime.of(2026, 7, 2, 12, 0)
        );

        assertThrows(DataIntegrityViolationException.class, () -> periodRepository.saveAndFlush(invalid));
    }

    @Test
    void rejectsIntervalsWhoseEndIsNotAfterStart() {
        LocalDateTime boundary = LocalDateTime.of(2026, 7, 2, 10, 0);
        ListingUnavailabilityPeriodEntity invalid = period(boundary, boundary);

        assertThrows(DataIntegrityViolationException.class, () -> periodRepository.saveAndFlush(invalid));
    }

    private ListingUnavailabilityPeriodEntity period(LocalDateTime startAt, LocalDateTime endAt) {
        return new ListingUnavailabilityPeriodEntity(listingId, startAt, endAt);
    }
}
