package ru.esie.practice.roomhubb2b.booking;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingStateMachineTest {

    private final BookingStateMachine stateMachine = new BookingStateMachine();

    @Test
    void allowsOnlyDocumentedTransitions() {
        Set<String> expected = Set.of(
                "REQUESTED->AWAITING_CONFIRMATION",
                "REQUESTED->REJECTED",
                "REQUESTED->CANCELLED",
                "AWAITING_CONFIRMATION->CONFIRMED",
                "AWAITING_CONFIRMATION->EXPIRED",
                "AWAITING_CONFIRMATION->CANCELLED",
                "CONFIRMED->IN_PROGRESS",
                "CONFIRMED->CANCELLED",
                "IN_PROGRESS->COMPLETED"
        );

        for (BookingStatus source : BookingStatus.values()) {
            for (BookingStatus target : BookingStatus.values()) {
                boolean expectedResult = expected.contains(source + "->" + target);
                assertThat(stateMachine.canTransition(source, target))
                        .as("%s -> %s", source, target)
                        .isEqualTo(expectedResult);
            }
        }
    }

    @Test
    void rejectsUndocumentedTransition() {
        assertThrows(
                BookingConflictException.class,
                () -> stateMachine.requireTransition(BookingStatus.REQUESTED, BookingStatus.COMPLETED)
        );
    }
}
