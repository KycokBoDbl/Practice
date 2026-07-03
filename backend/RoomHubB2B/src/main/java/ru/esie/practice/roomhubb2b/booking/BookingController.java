package ru.esie.practice.roomhubb2b.booking;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.esie.practice.roomhubb2b.booking.dto.BookingHistoryResponseDto;
import ru.esie.practice.roomhubb2b.booking.dto.BookingResponseDto;
import ru.esie.practice.roomhubb2b.booking.dto.CreateBookingRequestDto;

import java.util.List;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Authenticated booking workflow")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(operationId = "createBooking", summary = "Create a booking request")
    @BookingApiResponses
    public ResponseEntity<BookingResponseDto> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBookingRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.create(BookingActor.from(jwt), request));
    }

    @GetMapping("/{bookingId}")
    @Operation(operationId = "getBooking", summary = "Get a participant booking")
    @BookingApiResponses
    public BookingResponseDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookingId) {
        return bookingService.get(BookingActor.from(jwt), bookingId);
    }

    @GetMapping("/{bookingId}/history")
    @Operation(operationId = "getBookingHistory", summary = "Get booking status history")
    @BookingApiResponses
    public List<BookingHistoryResponseDto> history(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long bookingId
    ) {
        return bookingService.getHistory(BookingActor.from(jwt), bookingId);
    }

    @PostMapping("/{bookingId}/approve")
    @Operation(operationId = "approveBooking", summary = "Approve and hold a booking interval")
    @BookingApiResponses
    public BookingResponseDto approve(@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookingId) {
        return bookingService.approve(BookingActor.from(jwt), bookingId);
    }

    @PostMapping("/{bookingId}/reject")
    @Operation(operationId = "rejectBooking", summary = "Reject a booking request")
    @BookingApiResponses
    public BookingResponseDto reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookingId) {
        return bookingService.reject(BookingActor.from(jwt), bookingId);
    }

    @PostMapping("/{bookingId}/confirm")
    @Operation(operationId = "confirmBooking", summary = "Confirm an approved booking")
    @BookingApiResponses
    public BookingResponseDto confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookingId) {
        return bookingService.confirm(BookingActor.from(jwt), bookingId);
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(operationId = "cancelBooking", summary = "Cancel a future booking")
    @BookingApiResponses
    public BookingResponseDto cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long bookingId) {
        return bookingService.cancel(BookingActor.from(jwt), bookingId);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking operation completed"),
            @ApiResponse(responseCode = "201", description = "Booking created"),
            @ApiResponse(responseCode = "400", description = "Invalid booking request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Role cannot perform the operation",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Booking or listing not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Booking state or calendar conflict",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface BookingApiResponses {
    }
}
