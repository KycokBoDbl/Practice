package ru.esie.practice.roomhubb2b.booking;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import ru.esie.practice.roomhubb2b.booking.dto.BookingResponseDto;
import ru.esie.practice.roomhubb2b.booking.dto.CreateBookingRequestDto;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BookingDtoTest {

    @Test
    void validatesExactHourlyRequestFormat() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new CreateBookingRequestDto(
                    42L, "2030-01-01T10:00", "2030-01-01T11:00"
            ))).isEmpty();
            assertThat(validator.validate(new CreateBookingRequestDto(
                    42L, "2030-01-01T10:01", "2030-01-01T11:00"
            ))).isNotEmpty();
        }
    }

    @Test
    void serializesStableBookingFieldsAndMoney() throws Exception {
        BookingResponseDto response = new BookingResponseDto(
                81L,
                42L,
                BookingStatus.REQUESTED,
                "2030-01-01T10:00",
                "2030-01-01T13:00",
                new BigDecimal("2500.00"),
                new BigDecimal("7500.00"),
                null
        );

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains(
                "\"id\":81",
                "\"listingId\":42",
                "\"status\":\"REQUESTED\"",
                "\"startAt\":\"2030-01-01T10:00\"",
                "\"pricePerHour\":2500.00",
                "\"totalPrice\":7500.00",
                "\"confirmationDeadline\":null"
        );
    }
}
