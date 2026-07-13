package ru.esie.practice.roomhubb2b.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.esie.practice.roomhubb2b.auth.AccountNotFoundException;
import ru.esie.practice.roomhubb2b.auth.InvalidCredentialsException;
import ru.esie.practice.roomhubb2b.auth.RegistrationConflictException;
import ru.esie.practice.roomhubb2b.booking.BookingConflictException;
import ru.esie.practice.roomhubb2b.booking.BookingForbiddenException;
import ru.esie.practice.roomhubb2b.booking.BookingNotFoundException;
import ru.esie.practice.roomhubb2b.listing.ListingConflictException;
import ru.esie.practice.roomhubb2b.listing.ListingForbiddenException;
import ru.esie.practice.roomhubb2b.listing.ListingNotFoundException;
import ru.esie.practice.roomhubb2b.listing.geocoding.AddressNotGeocodedException;
import ru.esie.practice.roomhubb2b.listing.geocoding.GeocodingProviderException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage()
                ))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        InvalidFormatException invalidFormat = findCause(exception, InvalidFormatException.class);
        if (invalidFormat != null && invalidFormat.getTargetType() != null
                && invalidFormat.getTargetType().isEnum() && !invalidFormat.getPath().isEmpty()) {
            String field = invalidFormat.getPath().get(invalidFormat.getPath().size() - 1).getPropertyName();
            ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
            problem.setProperty("errors", List.of(Map.of(
                    "field", field == null ? "request" : field,
                    "message", "unsupported enum value"
            )));
            return problem;
        }
        return problem(HttpStatus.BAD_REQUEST, "Request body is malformed", request);
    }

    @ExceptionHandler(RegistrationConflictException.class)
    ProblemDetail handleConflict(RegistrationConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(BookingConflictException.class)
    ProblemDetail handleBookingConflict(BookingConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(ListingConflictException.class)
    ProblemDetail handleListingConflict(ListingConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(AddressNotGeocodedException.class)
    ProblemDetail handleAddressNotGeocoded(AddressNotGeocodedException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(GeocodingProviderException.class)
    ProblemDetail handleGeocodingProvider(GeocodingProviderException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(BookingNotFoundException.class)
    ProblemDetail handleBookingNotFound(BookingNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ListingNotFoundException.class)
    ProblemDetail handleListingNotFound(ListingNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(BookingForbiddenException.class)
    ProblemDetail handleBookingForbidden(BookingForbiddenException exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(ListingForbiddenException.class)
    ProblemDetail handleListingForbidden(ListingForbiddenException exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler({InvalidCredentialsException.class, AccountNotFoundException.class})
    ProblemDetail handleUnauthorized(RuntimeException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private static <T extends Throwable> T findCause(Throwable exception, Class<T> type) {
        Throwable current = exception;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
