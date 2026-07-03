package ru.esie.practice.roomhubb2b.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.auth.OrganizationRepository;
import ru.esie.practice.roomhubb2b.auth.UserRole;
import ru.esie.practice.roomhubb2b.booking.dto.BookingResponseDto;
import ru.esie.practice.roomhubb2b.booking.dto.CreateBookingRequestDto;
import ru.esie.practice.roomhubb2b.listing.ListingEntity;
import ru.esie.practice.roomhubb2b.listing.ListingRepository;
import ru.esie.practice.roomhubb2b.listing.ListingStatus;
import ru.esie.practice.roomhubb2b.listing.availability.ListingUnavailabilityPeriodRepository;
import ru.esie.practice.roomhubb2b.listing.availability.ListingAvailabilityService;

import java.time.LocalDateTime;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "roomhub.booking.scheduler-delay=PT24H")
@Import(BookingWorkflowIntegrationTest.ClockConfiguration.class)
@Transactional
class BookingWorkflowIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(2000);

    @Autowired
    private BookingService service;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingScheduler bookingScheduler;

    @Autowired
    private BookingStatusHistoryRepository historyRepository;

    @Autowired
    private ListingUnavailabilityPeriodRepository periodRepository;

    @Autowired
    private ListingAvailabilityService availabilityService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private MutableClock clock;

    private BookingActor landlord;
    private BookingActor tenant;
    private BookingActor outsider;
    private Long listingId;

    @BeforeEach
    void setUp() {
        clock.setInstant(Instant.parse("2030-01-01T00:00:00Z"));
        OrganizationEntity landlordOrganization = organization("Landlord");
        OrganizationEntity tenantOrganization = organization("Tenant");
        OrganizationEntity outsiderOrganization = organization("Outsider");
        landlord = new BookingActor(landlordOrganization.getId(), UserRole.LANDLORD);
        tenant = new BookingActor(tenantOrganization.getId(), UserRole.TENANT);
        outsider = new BookingActor(outsiderOrganization.getId(), UserRole.TENANT);

        ListingEntity listing = listingRepository.findByStatus(ListingStatus.PUBLISHED).get(0);
        listing.assignOwner(landlordOrganization);
        listingId = listingRepository.saveAndFlush(listing).getId();
    }

    @Test
    void runsWorkflowWithPriceSnapshotIdempotencyAndTimeTransitions() {
        BookingResponseDto created = create("2030-01-01T10:00", "2030-01-01T13:00");
        assertThat(created.status()).isEqualTo(BookingStatus.REQUESTED);
        assertThat(created.totalPrice()).isEqualByComparingTo(created.pricePerHour().multiply(java.math.BigDecimal.valueOf(3)));
        assertThat(historyRepository.countByBookingId(created.id())).isEqualTo(1);
        assertThat(bookingRepository.findByIdAndTenantOrganizationId(created.id(), tenant.organizationId()))
                .isPresent();
        assertThat(bookingRepository.findByIdAndLandlordOrganizationId(created.id(), landlord.organizationId()))
                .isPresent();
        assertThat(bookingRepository.findByIdAndTenantOrganizationId(created.id(), outsider.organizationId()))
                .isEmpty();

        BookingResponseDto approved = service.approve(landlord, created.id());
        assertThat(approved.status()).isEqualTo(BookingStatus.AWAITING_CONFIRMATION);
        assertThat(approved.confirmationDeadline()).isEqualTo("2030-01-01T00:30");
        service.approve(landlord, created.id());
        assertThat(periodRepository.countByBookingId(created.id())).isEqualTo(1);
        assertThat(historyRepository.countByBookingId(created.id())).isEqualTo(2);

        service.confirm(tenant, created.id());
        service.confirm(tenant, created.id());
        assertThat(historyRepository.countByBookingId(created.id())).isEqualTo(3);

        clock.setInstant(Instant.parse("2030-01-01T10:00:00Z"));
        assertThat(service.get(tenant, created.id()).status()).isEqualTo(BookingStatus.IN_PROGRESS);
        clock.setInstant(Instant.parse("2030-01-01T13:00:00Z"));
        assertThat(service.get(landlord, created.id()).status()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(historyRepository.findByBookingIdOrderByCreatedAtAscIdAsc(created.id()))
                .extracting(BookingStatusHistoryEntity::getToStatus)
                .containsExactly(
                        BookingStatus.REQUESTED,
                        BookingStatus.AWAITING_CONFIRMATION,
                        BookingStatus.CONFIRMED,
                        BookingStatus.IN_PROGRESS,
                        BookingStatus.COMPLETED
                );
        assertThat(periodRepository.countByBookingId(created.id())).isEqualTo(1);
    }

    @Test
    void expiresLateConfirmationAndReleasesInterval() {
        BookingResponseDto created = create("2030-01-01T10:00", "2030-01-01T11:00");
        service.approve(landlord, created.id());
        assertThat(availabilityService.getAvailability(
                listingId,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 1, 11, 0)
        ).busyIntervals()).hasSize(1);
        clock.setInstant(Instant.parse("2030-01-01T00:31:00Z"));

        assertThrows(BookingConflictException.class, () -> service.confirm(tenant, created.id()));
        assertThat(bookingRepository.findById(created.id()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.EXPIRED);
        assertThat(periodRepository.countByBookingId(created.id())).isZero();
        assertThat(availabilityService.getAvailability(
                listingId,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 1, 11, 0)
        ).busyIntervals()).isEmpty();
    }

    @Test
    void enforcesRoleAndOrganizationOwnership() {
        BookingResponseDto created = create("2030-01-01T10:00", "2030-01-01T11:00");

        assertThrows(BookingForbiddenException.class, () -> service.approve(tenant, created.id()));
        assertThrows(BookingNotFoundException.class, () -> service.get(outsider, created.id()));
        assertThrows(BookingForbiddenException.class, () -> service.create(
                landlord,
                new CreateBookingRequestDto(listingId, "2030-01-01T12:00", "2030-01-01T13:00")
        ));

        service.cancel(tenant, created.id());
        service.cancel(tenant, created.id());
        assertThat(historyRepository.countByBookingId(created.id())).isEqualTo(2);

        BookingResponseDto rejected = create("2030-01-01T12:00", "2030-01-01T13:00");
        service.reject(landlord, rejected.id());
        service.reject(landlord, rejected.id());
        assertThat(historyRepository.countByBookingId(rejected.id())).isEqualTo(2);
    }

    @Test
    void rejectsOverlappingButAllowsAdjacentIntervals() {
        BookingResponseDto first = create("2030-01-01T10:00", "2030-01-01T12:00");
        service.approve(landlord, first.id());
        BookingResponseDto overlapping = create("2030-01-01T11:00", "2030-01-01T13:00");
        BookingResponseDto adjacent = create("2030-01-01T12:00", "2030-01-01T13:00");

        assertThrows(BookingConflictException.class, () -> service.approve(landlord, overlapping.id()));
        assertThat(service.approve(landlord, adjacent.id()).status())
                .isEqualTo(BookingStatus.AWAITING_CONFIRMATION);
    }

    @Test
    void scheduledBatchProcessesOnlyDueBookings() {
        BookingResponseDto due = create("2030-01-01T10:00", "2030-01-01T11:00");
        service.approve(landlord, due.id());
        clock.setInstant(Instant.parse("2030-01-01T00:20:00Z"));
        BookingResponseDto notDue = create("2030-01-01T12:00", "2030-01-01T13:00");
        service.approve(landlord, notDue.id());
        clock.setInstant(Instant.parse("2030-01-01T00:31:00Z"));

        bookingScheduler.processDueBookings();

        assertThat(bookingRepository.findById(due.id()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.EXPIRED);
        assertThat(bookingRepository.findById(notDue.id()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.AWAITING_CONFIRMATION);
    }

    private BookingResponseDto create(String startAt, String endAt) {
        return service.create(tenant, new CreateBookingRequestDto(listingId, startAt, endAt));
    }

    private OrganizationEntity organization(String label) {
        int number = SEQUENCE.incrementAndGet();
        return organizationRepository.saveAndFlush(new OrganizationEntity(
                label + " " + number,
                String.format("78%08d", number)
        ));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
        }

    }
}
