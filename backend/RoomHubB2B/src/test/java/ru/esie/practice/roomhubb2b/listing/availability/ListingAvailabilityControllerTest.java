package ru.esie.practice.roomhubb2b.listing.availability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import ru.esie.practice.roomhubb2b.listing.availability.dto.BusyIntervalResponseDto;
import ru.esie.practice.roomhubb2b.listing.availability.dto.ListingAvailabilityResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListingAvailabilityControllerTest {

    private ListingAvailabilityService availabilityService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        availabilityService = mock(ListingAvailabilityService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ListingAvailabilityController(availabilityService))
                .build();
    }

    @Test
    void returnsHourlyAvailabilityJson() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 9, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 3, 18, 0);
        when(availabilityService.getAvailability(42L, from, to)).thenReturn(
                new ListingAvailabilityResponseDto(42L, from, to, List.of(
                        new BusyIntervalResponseDto(
                                LocalDateTime.of(2026, 7, 1, 12, 0),
                                LocalDateTime.of(2026, 7, 1, 15, 0)
                        ),
                        new BusyIntervalResponseDto(
                                LocalDateTime.of(2026, 7, 2, 10, 0),
                                LocalDateTime.of(2026, 7, 3, 11, 0)
                        )
                ))
        );

        mockMvc.perform(get("/api/listings/{listingId}/availability", 42L)
                        .queryParam("from", "2026-07-01T09:00")
                        .queryParam("to", "2026-07-03T18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.listingId").value(42))
                .andExpect(jsonPath("$.from").value("2026-07-01T09:00"))
                .andExpect(jsonPath("$.to").value("2026-07-03T18:00"))
                .andExpect(jsonPath("$.busyIntervals.length()").value(2))
                .andExpect(jsonPath("$.busyIntervals[0].length()").value(2))
                .andExpect(jsonPath("$.busyIntervals[0].startAt").value("2026-07-01T12:00"))
                .andExpect(jsonPath("$.busyIntervals[0].endAt").value("2026-07-01T15:00"))
                .andExpect(jsonPath("$.busyIntervals[1].startAt").value("2026-07-02T10:00"))
                .andExpect(jsonPath("$.busyIntervals[1].endAt").value("2026-07-03T11:00"));
    }

    @Test
    void returnsBadRequestForMissingOrMalformedParameters() throws Exception {
        mockMvc.perform(get("/api/listings/{listingId}/availability", 42L)
                        .queryParam("from", "2026-07-01T09:00"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/listings/{listingId}/availability", 42L)
                        .queryParam("from", "not-a-date")
                        .queryParam("to", "2026-07-03T18:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestWhenServiceRejectsTheRange() throws Exception {
        when(availabilityService.getAvailability(eq(42L), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));

        mockMvc.perform(get("/api/listings/{listingId}/availability", 42L)
                        .queryParam("from", "2026-07-01T09:01")
                        .queryParam("to", "2026-07-03T18:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingOrUnpublishedListing() throws Exception {
        when(availabilityService.getAvailability(eq(404L), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/listings/{listingId}/availability", 404L)
                        .queryParam("from", "2026-07-01T09:00")
                        .queryParam("to", "2026-07-03T18:00"))
                .andExpect(status().isNotFound());
    }
}
