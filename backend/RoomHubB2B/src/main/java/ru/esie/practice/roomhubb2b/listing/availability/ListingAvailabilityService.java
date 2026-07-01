package ru.esie.practice.roomhubb2b.listing.availability;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.esie.practice.roomhubb2b.listing.ListingRepository;
import ru.esie.practice.roomhubb2b.listing.ListingStatus;
import ru.esie.practice.roomhubb2b.listing.availability.dto.BusyIntervalResponseDto;
import ru.esie.practice.roomhubb2b.listing.availability.dto.ListingAvailabilityResponseDto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ListingAvailabilityService {

    private static final Duration MAX_RANGE = Duration.ofDays(93);

    private final ListingRepository listingRepository;
    private final ListingUnavailabilityPeriodRepository periodRepository;

    public ListingAvailabilityService(
            ListingRepository listingRepository,
            ListingUnavailabilityPeriodRepository periodRepository
    ) {
        this.listingRepository = listingRepository;
        this.periodRepository = periodRepository;
    }

    public ListingAvailabilityResponseDto getAvailability(
            Long listingId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        validateRange(from, to);

        if (!listingRepository.existsByIdAndStatus(listingId, ListingStatus.PUBLISHED)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found");
        }

        List<Interval> normalizedIntervals = normalize(
                periodRepository.findOverlapping(listingId, from, to),
                from,
                to
        );

        List<BusyIntervalResponseDto> busyIntervals = normalizedIntervals.stream()
                .map(interval -> new BusyIntervalResponseDto(interval.startAt(), interval.endAt()))
                .toList();

        return new ListingAvailabilityResponseDto(listingId, from, to, busyIntervals);
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw badRequest("Both from and to are required");
        }
        if (!isWholeHour(from) || !isWholeHour(to)) {
            throw badRequest("Range boundaries must be aligned to a whole hour");
        }
        if (!from.isBefore(to)) {
            throw badRequest("from must be earlier than to");
        }
        if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
            throw badRequest("Availability range must not exceed 93 days");
        }
    }

    private boolean isWholeHour(LocalDateTime value) {
        return value.getMinute() == 0 && value.getSecond() == 0 && value.getNano() == 0;
    }

    private List<Interval> normalize(
            List<ListingUnavailabilityPeriodEntity> periods,
            LocalDateTime from,
            LocalDateTime to
    ) {
        List<Interval> clipped = periods.stream()
                .map(period -> new Interval(
                        max(period.getStartAt(), from),
                        min(period.getEndAt(), to)
                ))
                .filter(interval -> interval.startAt().isBefore(interval.endAt()))
                .sorted(Comparator.comparing(Interval::startAt).thenComparing(Interval::endAt))
                .toList();

        List<Interval> merged = new ArrayList<>();
        for (Interval next : clipped) {
            if (merged.isEmpty()) {
                merged.add(next);
                continue;
            }

            int lastIndex = merged.size() - 1;
            Interval current = merged.get(lastIndex);
            if (!next.startAt().isAfter(current.endAt())) {
                merged.set(lastIndex, new Interval(current.startAt(), max(current.endAt(), next.endAt())));
            } else {
                merged.add(next);
            }
        }
        return merged;
    }

    private LocalDateTime min(LocalDateTime first, LocalDateTime second) {
        return first.isBefore(second) ? first : second;
    }

    private LocalDateTime max(LocalDateTime first, LocalDateTime second) {
        return first.isAfter(second) ? first : second;
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private record Interval(LocalDateTime startAt, LocalDateTime endAt) {
    }
}
