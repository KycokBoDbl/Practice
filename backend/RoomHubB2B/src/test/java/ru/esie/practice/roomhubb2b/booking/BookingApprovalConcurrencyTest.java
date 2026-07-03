package ru.esie.practice.roomhubb2b.booking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.auth.OrganizationRepository;
import ru.esie.practice.roomhubb2b.auth.UserRole;
import ru.esie.practice.roomhubb2b.booking.dto.BookingResponseDto;
import ru.esie.practice.roomhubb2b.booking.dto.CreateBookingRequestDto;
import ru.esie.practice.roomhubb2b.listing.ListingEntity;
import ru.esie.practice.roomhubb2b.listing.ListingRepository;
import ru.esie.practice.roomhubb2b.listing.ListingStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "roomhub.booking.scheduler-delay=PT24H")
class BookingApprovalConcurrencyTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(4000);

    @Autowired
    private BookingService service;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> bookingIds = new ArrayList<>();
    private final List<Long> organizationIds = new ArrayList<>();
    private Long listingId;
    private BookingActor landlordActor;
    private BookingActor tenantActor;

    @BeforeEach
    void setUp() {
        OrganizationEntity landlord = organization("Concurrency landlord");
        OrganizationEntity tenant = organization("Concurrency tenant");
        landlordActor = new BookingActor(landlord.getId(), UserRole.LANDLORD);
        tenantActor = new BookingActor(tenant.getId(), UserRole.TENANT);
        ListingEntity listing = listingRepository.findByStatus(ListingStatus.PUBLISHED).get(0);
        listing.assignOwner(landlord);
        listingId = listingRepository.saveAndFlush(listing).getId();
    }

    @AfterEach
    void cleanUp() {
        for (Long bookingId : bookingIds) {
            jdbcTemplate.update("DELETE FROM listing_unavailability_periods WHERE booking_id = ?", bookingId);
            jdbcTemplate.update("DELETE FROM booking_status_history WHERE booking_id = ?", bookingId);
            jdbcTemplate.update("DELETE FROM bookings WHERE id = ?", bookingId);
        }
        if (listingId != null) {
            jdbcTemplate.update("UPDATE listings SET owner_organization_id = NULL WHERE id = ?", listingId);
        }
        for (Long organizationId : organizationIds) {
            jdbcTemplate.update("DELETE FROM organizations WHERE id = ?", organizationId);
        }
    }

    @Test
    void grantsExactlyOneOverlappingInterval() throws Exception {
        bookingIds.add(service.create(tenantActor, request("2035-02-01T10:00", "2035-02-01T12:00")).id());
        bookingIds.add(service.create(tenantActor, request("2035-02-01T11:00", "2035-02-01T13:00")).id());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<BookingResponseDto>> futures = bookingIds.stream()
                    .map(id -> executor.submit(() -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        return service.approve(landlordActor, id);
                    }))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successes = 0;
            int conflicts = 0;
            for (Future<BookingResponseDto> future : futures) {
                try {
                    assertThat(future.get(10, TimeUnit.SECONDS).status())
                            .isEqualTo(BookingStatus.AWAITING_CONFIRMATION);
                    successes++;
                } catch (ExecutionException exception) {
                    assertThat(exception.getCause()).isInstanceOf(BookingConflictException.class);
                    conflicts++;
                }
            }
            assertThat(successes).isEqualTo(1);
            assertThat(conflicts).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }

        Integer blocks = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM listing_unavailability_periods WHERE booking_id IN (?, ?)",
                Integer.class,
                bookingIds.get(0),
                bookingIds.get(1)
        );
        assertThat(blocks).isEqualTo(1);
    }

    private CreateBookingRequestDto request(String startAt, String endAt) {
        return new CreateBookingRequestDto(listingId, startAt, endAt);
    }

    private OrganizationEntity organization(String name) {
        int number = SEQUENCE.incrementAndGet();
        OrganizationEntity organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                name,
                String.format("76%08d", number)
        ));
        organizationIds.add(organization.getId());
        return organization;
    }
}
