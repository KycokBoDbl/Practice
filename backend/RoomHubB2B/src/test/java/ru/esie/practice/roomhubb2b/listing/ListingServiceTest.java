package ru.esie.practice.roomhubb2b.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.auth.OrganizationRepository;
import ru.esie.practice.roomhubb2b.auth.UserRole;
import ru.esie.practice.roomhubb2b.booking.BookingRepository;
import ru.esie.practice.roomhubb2b.listing.dto.CreateListingRequestDto;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;
import ru.esie.practice.roomhubb2b.listing.dto.UpdateListingRequestDto;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListingServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-07T10:15:30Z");

    private ListingRepository listingRepository;
    private OrganizationRepository organizationRepository;
    private BookingRepository bookingRepository;
    private ListingService service;

    @BeforeEach
    void setUp() {
        listingRepository = mock(ListingRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        bookingRepository = mock(BookingRepository.class);
        service = new ListingService(
                listingRepository,
                organizationRepository,
                bookingRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void publishesListingWithServerControlledFields() {
        OrganizationEntity owner = new OrganizationEntity("Landlord LLC", "7700000001");
        when(organizationRepository.findById(17L)).thenReturn(Optional.of(owner));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ListingResponseDto response = service.publish(
                new ListingActor(17L, UserRole.LANDLORD),
                request()
        );

        ArgumentCaptor<ListingEntity> captor = ArgumentCaptor.forClass(ListingEntity.class);
        verify(listingRepository).save(captor.capture());
        ListingEntity saved = captor.getValue();
        assertThat(saved.getOwnerOrganization()).isSameAs(owner);
        assertThat(saved.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(saved.getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(response.title()).isEqualTo("Meeting room");
        assertThat(response.pricePerHour()).isEqualByComparingTo("2500.00");
    }

    @Test
    void rejectsTenantWithoutLookingUpOrganizationOrSaving() {
        assertThatThrownBy(() -> service.publish(
                new ListingActor(17L, UserRole.TENANT),
                request()
        )).isInstanceOf(ListingForbiddenException.class);

        verify(organizationRepository, never()).findById(any());
        verify(listingRepository, never()).save(any());
    }

    @Test
    void updatesOwnedListingWithoutChangingServerControlledFields() {
        ListingEntity listing = ownedListing();
        LocalDateTime createdAt = listing.getCreatedAt();
        OrganizationEntity owner = listing.getOwnerOrganization();
        when(listingRepository.findByIdAndOwnerOrganizationId(99L, 17L))
                .thenReturn(Optional.of(listing));

        ListingResponseDto response = service.update(
                new ListingActor(17L, UserRole.LANDLORD),
                99L,
                updateRequest()
        );

        assertThat(listing.getTitle()).isEqualTo("Updated room");
        assertThat(listing.getDescription()).isNull();
        assertThat(listing.getCity()).isEqualTo("Novosibirsk");
        assertThat(listing.getAddress()).isEqualTo("Krasny Avenue, 1");
        assertThat(listing.getPricePerHour()).isEqualByComparingTo("3000.00");
        assertThat(listing.getCapacity()).isEqualTo(24);
        assertThat(listing.getSpaceType()).isEqualTo(SpaceType.CONFERENCE_HALL);
        assertThat(listing.getImageUrl()).isNull();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(listing.getCreatedAt()).isEqualTo(createdAt);
        assertThat(listing.getOwnerOrganization()).isSameAs(owner);
        assertThat(response.title()).isEqualTo("Updated room");
    }

    @Test
    void rejectsTenantManagementBeforeLookup() {
        assertThatThrownBy(() -> service.hide(
                new ListingActor(17L, UserRole.TENANT),
                99L
        )).isInstanceOf(ListingForbiddenException.class);

        verify(listingRepository, never()).findByIdAndOwnerOrganizationId(any(), any());
    }

    @Test
    void returnsNotFoundForNonOwnedListing() {
        when(listingRepository.findByIdAndOwnerOrganizationId(99L, 17L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(
                new ListingActor(17L, UserRole.LANDLORD),
                99L
        )).isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    void hidesAndReactivatesOwnedListingIdempotently() {
        ListingEntity listing = ownedListing();
        when(listingRepository.findByIdAndOwnerOrganizationId(99L, 17L))
                .thenReturn(Optional.of(listing));

        service.hide(new ListingActor(17L, UserRole.LANDLORD), 99L);
        service.hide(new ListingActor(17L, UserRole.LANDLORD), 99L);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.ARCHIVED);

        service.activate(new ListingActor(17L, UserRole.LANDLORD), 99L);
        service.activate(new ListingActor(17L, UserRole.LANDLORD), 99L);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
    }

    @Test
    void deletesOwnedListingWithoutBookings() {
        ListingEntity listing = ownedListing();
        ReflectionTestUtils.setField(listing, "id", 99L);
        when(listingRepository.findLockedById(99L))
                .thenReturn(Optional.of(listing));
        when(bookingRepository.existsByListingId(99L)).thenReturn(false);

        service.delete(new ListingActor(17L, UserRole.LANDLORD), 99L);

        verify(bookingRepository).existsByListingId(99L);
        verify(listingRepository).delete(listing);
        verify(listingRepository).flush();
    }

    @Test
    void rejectsDeleteWhenBookingHistoryExists() {
        ListingEntity listing = ownedListing();
        ReflectionTestUtils.setField(listing, "id", 99L);
        when(listingRepository.findLockedById(99L))
                .thenReturn(Optional.of(listing));
        when(bookingRepository.existsByListingId(99L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(new ListingActor(17L, UserRole.LANDLORD), 99L))
                .isInstanceOf(ListingConflictException.class);

        verify(bookingRepository).existsByListingId(99L);
        verify(listingRepository, never()).delete(any());
    }

    @Test
    void mapsDeleteConstraintRaceToConflict() {
        ListingEntity listing = ownedListing();
        ReflectionTestUtils.setField(listing, "id", 99L);
        when(listingRepository.findLockedById(99L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByListingId(99L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("booking FK race"))
                .when(listingRepository).flush();

        assertThatThrownBy(() -> service.delete(new ListingActor(17L, UserRole.LANDLORD), 99L))
                .isInstanceOf(ListingConflictException.class)
                .hasMessage("Listing has booking history and cannot be deleted");

        verify(listingRepository).delete(listing);
        verify(listingRepository).flush();
    }

    private CreateListingRequestDto request() {
        return new CreateListingRequestDto(
                "Meeting room",
                "Description",
                "Barnaul",
                "Lenina Avenue, 10",
                new BigDecimal("2500.00"),
                20,
                SpaceType.MEETING_ROOM,
                "https://example.com/listing.jpg"
        );
    }

    private UpdateListingRequestDto updateRequest() {
        return new UpdateListingRequestDto(
                "Updated room",
                null,
                "Novosibirsk",
                "Krasny Avenue, 1",
                new BigDecimal("3000.00"),
                24,
                SpaceType.CONFERENCE_HALL,
                null
        );
    }

    private ListingEntity ownedListing() {
        OrganizationEntity owner = new OrganizationEntity("Landlord LLC", "7700000001");
        ReflectionTestUtils.setField(owner, "id", 17L);
        return ListingEntity.published(
                "Meeting room",
                "Description",
                "Barnaul",
                "Lenina Avenue, 10",
                new BigDecimal("2500.00"),
                20,
                SpaceType.MEETING_ROOM,
                "https://example.com/listing.jpg",
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC),
                owner
        );
    }
}
