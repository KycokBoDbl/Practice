package ru.esie.practice.roomhubb2b.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.auth.OrganizationRepository;
import ru.esie.practice.roomhubb2b.auth.UserRole;
import ru.esie.practice.roomhubb2b.listing.dto.CreateListingRequestDto;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListingServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-07T10:15:30Z");

    private ListingRepository listingRepository;
    private OrganizationRepository organizationRepository;
    private ListingService service;

    @BeforeEach
    void setUp() {
        listingRepository = mock(ListingRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        service = new ListingService(
                listingRepository,
                organizationRepository,
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
}
