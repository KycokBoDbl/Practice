package ru.esie.practice.roomhubb2b.booking;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

@Component
public class BookingStateMachine {

    private final Map<BookingStatus, EnumSet<BookingStatus>> transitions;

    public BookingStateMachine() {
        transitions = new EnumMap<>(BookingStatus.class);
        allow(BookingStatus.REQUESTED,
                BookingStatus.AWAITING_CONFIRMATION,
                BookingStatus.REJECTED,
                BookingStatus.CANCELLED);
        allow(BookingStatus.AWAITING_CONFIRMATION,
                BookingStatus.CONFIRMED,
                BookingStatus.EXPIRED,
                BookingStatus.CANCELLED);
        allow(BookingStatus.CONFIRMED,
                BookingStatus.IN_PROGRESS,
                BookingStatus.CANCELLED);
        allow(BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);
    }

    public boolean canTransition(BookingStatus source, BookingStatus target) {
        return transitions.getOrDefault(source, EnumSet.noneOf(BookingStatus.class)).contains(target);
    }

    public void requireTransition(BookingStatus source, BookingStatus target) {
        if (!canTransition(source, target)) {
            throw new BookingConflictException(
                    "Booking cannot transition from " + source + " to " + target
            );
        }
    }

    private void allow(BookingStatus source, BookingStatus... targets) {
        transitions.put(source, EnumSet.of(targets[0], targets));
    }
}
