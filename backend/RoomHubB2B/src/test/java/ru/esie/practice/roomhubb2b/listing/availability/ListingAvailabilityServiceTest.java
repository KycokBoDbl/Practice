package ru.esie.practice.roomhubb2b.listing.availability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.esie.practice.roomhubb2b.listing.ListingRepository;
import ru.esie.practice.roomhubb2b.listing.availability.dto.BusyIntervalResponseDto;
import ru.esie.practice.roomhubb2b.listing.availability.dto.ListingAvailabilityResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListingAvailabilityServiceTest {

    private static final Long LISTING_ID = 42L;
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 7, 2, 9, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 7, 2, 18, 0);

    private ListingRepository listingRepository;
    private ListingUnavailabilityPeriodRepository periodRepository;
    private ListingAvailabilityService service;

    @BeforeEach
    void setUp() {
        listingRepository = mock(ListingRepository.class);
        periodRepository = mock(ListingUnavailabilityPeriodRepository.class);
        service = new ListingAvailabilityService(listingRepository, periodRepository);
        when(listingRepository.existsByIdAndStatus(LISTING_ID, "PUBLISHED")).thenReturn(true);
    }

    @Test
    void returnsEmptyBusyIntervalsForAFreeRange() {
        when(periodRepository.findOverlapping(LISTING_ID, FROM, TO)).thenReturn(List.of());

        ListingAvailabilityResponseDto response = service.getAvailability(LISTING_ID, FROM, TO);

        assertEquals(LISTING_ID, response.getListingId());
        assertEquals("2026-07-02T09:00", response.getFrom());
        assertEquals("2026-07-02T18:00", response.getTo());
        assertEquals(List.of(), response.getBusyIntervals());
    }

    @Test
    void clipsSortsAndMergesOverlappingAndAdjacentIntervals() {
        when(periodRepository.findOverlapping(LISTING_ID, FROM, TO)).thenReturn(List.of(
                period(TO, TO.plusHours(2)),
                period(FROM.plusHours(4), FROM.plusHours(6)),
                period(FROM.minusHours(2), FROM.plusHours(1)),
                period(FROM.plusHours(1), FROM.plusHours(3)),
                period(FROM.plusHours(2), FROM.plusHours(4)),
                period(FROM.plusHours(8), TO.plusHours(2)),
                period(FROM.minusHours(2), FROM)
        ));

        ListingAvailabilityResponseDto response = service.getAvailability(LISTING_ID, FROM, TO);

        assertEquals(2, response.getBusyIntervals().size());
        assertInterval(response.getBusyIntervals().get(0), "2026-07-02T09:00", "2026-07-02T15:00");
        assertInterval(response.getBusyIntervals().get(1), "2026-07-02T17:00", "2026-07-02T18:00");
    }

    @Test
    void keepsAnIntervalAcrossMidnightContinuous() {
        LocalDateTime from = LocalDateTime.of(2026, 7, 2, 20, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 3, 4, 0);
        when(periodRepository.findOverlapping(LISTING_ID, from, to)).thenReturn(List.of(
                period(
                        LocalDateTime.of(2026, 7, 2, 22, 0),
                        LocalDateTime.of(2026, 7, 3, 2, 0)
                )
        ));

        ListingAvailabilityResponseDto response = service.getAvailability(LISTING_ID, from, to);

        assertEquals(1, response.getBusyIntervals().size());
        assertInterval(response.getBusyIntervals().get(0), "2026-07-02T22:00", "2026-07-03T02:00");
    }

    @Test
    void acceptsExactlyNinetyThreeDays() {
        LocalDateTime to = FROM.plusDays(93);
        when(periodRepository.findOverlapping(LISTING_ID, FROM, to)).thenReturn(List.of());

        ListingAvailabilityResponseDto response = service.getAvailability(LISTING_ID, FROM, to);

        assertEquals("2026-10-03T09:00", response.getTo());
    }

    @Test
    void rejectsBoundariesThatAreNotWholeHours() {
        assertBadRequest(FROM.plusMinutes(1), TO);
        assertBadRequest(FROM.plusSeconds(1), TO);
        assertBadRequest(FROM.plusNanos(1), TO);
    }

    @Test
    void rejectsEmptyAndReverseRanges() {
        assertBadRequest(FROM, FROM);
        assertBadRequest(TO, FROM);
    }

    @Test
    void rejectsRangesLongerThanNinetyThreeDays() {
        assertBadRequest(FROM, FROM.plusDays(93).plusHours(1));
    }

    @Test
    void returnsNotFoundForMissingOrUnpublishedListing() {
        when(listingRepository.existsByIdAndStatus(LISTING_ID, "PUBLISHED")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getAvailability(LISTING_ID, FROM, TO)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(periodRepository, never()).findOverlapping(any(), any(), any());
    }

    private ListingUnavailabilityPeriodEntity period(LocalDateTime startAt, LocalDateTime endAt) {
        return new ListingUnavailabilityPeriodEntity(LISTING_ID, startAt, endAt);
    }

    private void assertBadRequest(LocalDateTime from, LocalDateTime to) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getAvailability(LISTING_ID, from, to)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(listingRepository, never()).existsByIdAndStatus(eq(LISTING_ID), any());
    }

    private void assertInterval(BusyIntervalResponseDto interval, String startAt, String endAt) {
        assertEquals(startAt, interval.getStartAt());
        assertEquals(endAt, interval.getEndAt());
    }
}
