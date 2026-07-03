package ru.esie.practice.roomhubb2b.booking;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.esie.practice.roomhubb2b.config.BookingProperties;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class BookingScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final BookingProperties properties;
    private final Clock clock;

    public BookingScheduler(
            BookingRepository bookingRepository,
            BookingService bookingService,
            BookingProperties properties,
            Clock clock
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${roomhub.booking.scheduler-delay:PT30S}",
            fixedDelayString = "${roomhub.booking.scheduler-delay:PT30S}"
    )
    public void processDueBookings() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        List<Long> dueIds = bookingRepository.findDueIds(
                now,
                PageRequest.of(0, properties.batchSize())
        );
        dueIds.forEach(bookingService::synchronizeDueById);
    }
}
