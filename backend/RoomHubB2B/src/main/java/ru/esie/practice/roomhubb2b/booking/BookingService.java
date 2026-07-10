package ru.esie.practice.roomhubb2b.booking;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.auth.OrganizationRepository;
import ru.esie.practice.roomhubb2b.auth.UserRole;
import ru.esie.practice.roomhubb2b.booking.dto.BookingInboxItemDto;
import ru.esie.practice.roomhubb2b.booking.dto.BookingHistoryResponseDto;
import ru.esie.practice.roomhubb2b.booking.dto.BookingResponseDto;
import ru.esie.practice.roomhubb2b.booking.dto.CreateBookingRequestDto;
import ru.esie.practice.roomhubb2b.config.BookingProperties;
import ru.esie.practice.roomhubb2b.listing.ListingEntity;
import ru.esie.practice.roomhubb2b.listing.ListingRepository;
import ru.esie.practice.roomhubb2b.listing.ListingStatus;
import ru.esie.practice.roomhubb2b.listing.availability.ListingUnavailabilityPeriodEntity;
import ru.esie.practice.roomhubb2b.listing.availability.ListingUnavailabilityPeriodRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class BookingService {

    private static final DateTimeFormatter HOUR_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm");
    private static final String DUPLICATE_BOOKING_CONSTRAINT = "uk_bookings_tenant_listing_start_date";
    private static final String DUPLICATE_BOOKING_MESSAGE =
            "Tenant already has a booking request for this listing on this date";

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final ListingRepository listingRepository;
    private final OrganizationRepository organizationRepository;
    private final ListingUnavailabilityPeriodRepository periodRepository;
    private final BookingStateMachine stateMachine;
    private final BookingProperties properties;
    private final Clock clock;

    public BookingService(
            BookingRepository bookingRepository,
            BookingStatusHistoryRepository historyRepository,
            ListingRepository listingRepository,
            OrganizationRepository organizationRepository,
            ListingUnavailabilityPeriodRepository periodRepository,
            BookingStateMachine stateMachine,
            BookingProperties properties,
            Clock clock
    ) {
        this.bookingRepository = bookingRepository;
        this.historyRepository = historyRepository;
        this.listingRepository = listingRepository;
        this.organizationRepository = organizationRepository;
        this.periodRepository = periodRepository;
        this.stateMachine = stateMachine;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public BookingResponseDto create(BookingActor actor, CreateBookingRequestDto request) {
        requireRole(actor, UserRole.TENANT);
        LocalDateTime startAt = parseHour(request.startAt());
        LocalDateTime endAt = parseHour(request.endAt());
        LocalDateTime now = now();
        validatePeriod(startAt, endAt, now);

        ListingEntity listing = listingRepository.findLockedById(request.listingId())
                .filter(candidate -> candidate.getStatus() == ListingStatus.PUBLISHED)
                .filter(candidate -> candidate.getOwnerOrganization() != null)
                .orElseThrow(() -> new BookingNotFoundException("Listing not found"));
        OrganizationEntity tenant = organizationRepository.findById(actor.organizationId())
                .orElseThrow(() -> new BookingNotFoundException("Tenant organization not found"));
        rejectDuplicateApplication(actor.organizationId(), listing.getId(), startAt);

        long hours = Duration.between(startAt, endAt).toHours();
        BigDecimal rate = listing.getPricePerHour();
        BookingEntity booking = saveBooking(listing, tenant, startAt, endAt, rate, hours, now);
        historyRepository.save(new BookingStatusHistoryEntity(
                booking, null, BookingStatus.REQUESTED, "CREATE", now
        ));
        return BookingResponseDto.from(booking);
    }

    @Transactional
    public BookingResponseDto get(BookingActor actor, Long bookingId) {
        BookingEntity booking = loadLockedForParticipant(actor, bookingId);
        synchronizeDue(booking, now());
        return BookingResponseDto.from(booking);
    }

    @Transactional
    public List<BookingHistoryResponseDto> getHistory(BookingActor actor, Long bookingId) {
        BookingEntity booking = loadLockedForParticipant(actor, bookingId);
        synchronizeDue(booking, now());
        return historyRepository.findByBookingIdOrderByCreatedAtAscIdAsc(bookingId).stream()
                .map(BookingHistoryResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingInboxItemDto> getInbox(BookingActor actor) {
        if (actor.role() == UserRole.TENANT) {
            return bookingRepository.findInboxByTenantOrganizationId(actor.organizationId()).stream()
                    .map(BookingInboxItemDto::from)
                    .toList();
        }
        if (actor.role() == UserRole.LANDLORD) {
            return bookingRepository.findInboxByLandlordOrganizationId(actor.organizationId()).stream()
                    .map(BookingInboxItemDto::from)
                    .toList();
        }
        throw new BookingForbiddenException("Role cannot access bookings");
    }

    @Transactional
    public BookingResponseDto approve(BookingActor actor, Long bookingId) {
        requireRole(actor, UserRole.LANDLORD);
        BookingEntity booking = loadLocked(bookingId);
        requireLandlordOwnership(actor, booking);
        LocalDateTime now = now();
        synchronizeDue(booking, now);
        if (booking.getStatus() == BookingStatus.AWAITING_CONFIRMATION) {
            return BookingResponseDto.from(booking);
        }
        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw conflict(booking, "approve");
        }

        ListingEntity listing = listingRepository.findLockedById(booking.getListing().getId())
                .orElseThrow(() -> new BookingNotFoundException("Listing not found"));
        if (listing.getStatus() != ListingStatus.PUBLISHED
                || listing.getOwnerOrganization() == null
                || !listing.getOwnerOrganization().getId().equals(actor.organizationId())) {
            throw new BookingNotFoundException("Booking not found");
        }
        if (!booking.getStartAt().isAfter(now)) {
            throw new BookingConflictException("Booking interval has already started");
        }
        if (periodRepository.existsOverlap(listing.getId(), booking.getStartAt(), booking.getEndAt())) {
            throw new BookingConflictException("Booking interval is no longer available");
        }

        periodRepository.save(new ListingUnavailabilityPeriodEntity(booking));
        LocalDateTime deadline = min(
                now.plus(properties.confirmationHold()),
                booking.getStartAt()
        );
        transition(booking, BookingStatus.AWAITING_CONFIRMATION, "APPROVE", deadline, now);
        return BookingResponseDto.from(booking);
    }

    @Transactional
    public BookingResponseDto reject(BookingActor actor, Long bookingId) {
        requireRole(actor, UserRole.LANDLORD);
        BookingEntity booking = loadLocked(bookingId);
        requireLandlordOwnership(actor, booking);
        LocalDateTime now = now();
        synchronizeDue(booking, now);
        if (booking.getStatus() == BookingStatus.REJECTED) {
            return BookingResponseDto.from(booking);
        }
        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw conflict(booking, "reject");
        }
        transition(booking, BookingStatus.REJECTED, "REJECT", null, now);
        return BookingResponseDto.from(booking);
    }

    @Transactional(noRollbackFor = BookingConflictException.class)
    public BookingResponseDto confirm(BookingActor actor, Long bookingId) {
        requireRole(actor, UserRole.TENANT);
        BookingEntity booking = loadLocked(bookingId);
        requireTenantOwnership(actor, booking);
        LocalDateTime now = now();
        synchronizeDue(booking, now);
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return BookingResponseDto.from(booking);
        }
        if (booking.getStatus() != BookingStatus.AWAITING_CONFIRMATION) {
            throw conflict(booking, "confirm");
        }
        transition(booking, BookingStatus.CONFIRMED, "CONFIRM", booking.getConfirmationDeadline(), now);
        return BookingResponseDto.from(booking);
    }

    @Transactional(noRollbackFor = BookingConflictException.class)
    public BookingResponseDto cancel(BookingActor actor, Long bookingId) {
        requireRole(actor, UserRole.TENANT);
        BookingEntity booking = loadLocked(bookingId);
        requireTenantOwnership(actor, booking);
        LocalDateTime now = now();
        synchronizeDue(booking, now);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return BookingResponseDto.from(booking);
        }
        if (booking.getStatus() != BookingStatus.REQUESTED
                && booking.getStatus() != BookingStatus.AWAITING_CONFIRMATION
                && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw conflict(booking, "cancel");
        }
        if (!booking.getStartAt().isAfter(now)) {
            synchronizeDue(booking, now);
            throw conflict(booking, "cancel");
        }
        periodRepository.deleteByBookingId(bookingId);
        transition(booking, BookingStatus.CANCELLED, "CANCEL", null, now);
        return BookingResponseDto.from(booking);
    }

    @Transactional
    public void synchronizeDueById(Long bookingId) {
        bookingRepository.findLockedById(bookingId).ifPresent(booking -> synchronizeDue(booking, now()));
    }

    private BookingEntity loadLockedForParticipant(BookingActor actor, Long bookingId) {
        BookingEntity booking = loadLocked(bookingId);
        if (actor.role() == UserRole.TENANT) {
            requireTenantOwnership(actor, booking);
        } else if (actor.role() == UserRole.LANDLORD) {
            requireLandlordOwnership(actor, booking);
        } else {
            throw new BookingForbiddenException("Role cannot access bookings");
        }
        return booking;
    }

    private BookingEntity loadLocked(Long bookingId) {
        return bookingRepository.findLockedById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
    }

    private void requireRole(BookingActor actor, UserRole role) {
        if (actor.role() != role) {
            throw new BookingForbiddenException("Role " + actor.role() + " cannot perform this operation");
        }
    }

    private void requireTenantOwnership(BookingActor actor, BookingEntity booking) {
        if (!booking.getTenantOrganization().getId().equals(actor.organizationId())) {
            throw new BookingNotFoundException("Booking not found");
        }
    }

    private void requireLandlordOwnership(BookingActor actor, BookingEntity booking) {
        OrganizationEntity owner = booking.getListing().getOwnerOrganization();
        if (owner == null || !owner.getId().equals(actor.organizationId())) {
            throw new BookingNotFoundException("Booking not found");
        }
    }

    private void synchronizeDue(BookingEntity booking, LocalDateTime now) {
        if (booking.getStatus() == BookingStatus.AWAITING_CONFIRMATION
                && !booking.getConfirmationDeadline().isAfter(now)) {
            periodRepository.deleteByBookingId(booking.getId());
            transition(booking, BookingStatus.EXPIRED, "EXPIRE", null, now);
            return;
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED && !booking.getStartAt().isAfter(now)) {
            transition(booking, BookingStatus.IN_PROGRESS, "START", booking.getConfirmationDeadline(), now);
        }
        if (booking.getStatus() == BookingStatus.IN_PROGRESS && !booking.getEndAt().isAfter(now)) {
            transition(booking, BookingStatus.COMPLETED, "COMPLETE", booking.getConfirmationDeadline(), now);
        }
    }

    private void transition(
            BookingEntity booking,
            BookingStatus target,
            String reason,
            LocalDateTime deadline,
            LocalDateTime now
    ) {
        BookingStatus source = booking.getStatus();
        stateMachine.requireTransition(source, target);
        booking.transitionTo(target, deadline, now);
        historyRepository.save(new BookingStatusHistoryEntity(booking, source, target, reason, now));
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt, LocalDateTime now) {
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("endAt must be later than startAt");
        }
        if (!startAt.isAfter(now)) {
            throw new IllegalArgumentException("startAt must be in the future");
        }
    }

    private void rejectDuplicateApplication(Long tenantOrganizationId, Long listingId, LocalDateTime startAt) {
        LocalDateTime dayStart = startAt.toLocalDate().atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);
        if (bookingRepository.existsByTenantOrganizationIdAndListingIdAndStartAtGreaterThanEqualAndStartAtLessThan(
                tenantOrganizationId,
                listingId,
                dayStart,
                nextDayStart
        )) {
            throw duplicateApplication();
        }
    }

    private BookingEntity saveBooking(
            ListingEntity listing,
            OrganizationEntity tenant,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BigDecimal rate,
            long hours,
            LocalDateTime now
    ) {
        try {
            return bookingRepository.saveAndFlush(new BookingEntity(
                    listing,
                    tenant,
                    startAt,
                    endAt,
                    rate,
                    rate.multiply(BigDecimal.valueOf(hours)),
                    now
            ));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateApplicationConstraint(exception)) {
                throw duplicateApplication();
            }
            throw exception;
        }
    }

    private boolean isDuplicateApplicationConstraint(Throwable exception) {
        ConstraintViolationException constraintViolation =
                findCause(exception, ConstraintViolationException.class);
        return constraintViolation != null
                && DUPLICATE_BOOKING_CONSTRAINT.equals(constraintViolation.getConstraintName());
    }

    private LocalDateTime parseHour(String value) {
        try {
            return LocalDateTime.parse(value, HOUR_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Booking time must use YYYY-MM-DDTHH:00");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private LocalDateTime min(LocalDateTime first, LocalDateTime second) {
        return first.isBefore(second) ? first : second;
    }

    private BookingConflictException conflict(BookingEntity booking, String command) {
        return new BookingConflictException(
                "Cannot " + command + " booking in status " + booking.getStatus()
        );
    }

    private BookingConflictException duplicateApplication() {
        return new BookingConflictException(DUPLICATE_BOOKING_MESSAGE);
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
