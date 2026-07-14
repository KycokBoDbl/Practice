package ru.esie.practice.roomhubb2b.listing.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.listing.ListingEntity;
import ru.esie.practice.roomhubb2b.listing.ListingRepository;
import ru.esie.practice.roomhubb2b.listing.ListingStatus;
import ru.esie.practice.roomhubb2b.listing.SpaceType;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiListingSearchServiceTest {

    private ListingRepository listingRepository;
    private GigaChatCompletionClient gigaChatClient;
    private GigaChatProperties properties;
    private AiListingSearchService service;

    @BeforeEach
    void setUp() {
        listingRepository = mock(ListingRepository.class);
        gigaChatClient = mock(GigaChatCompletionClient.class);
        properties = properties();
        service = new AiListingSearchService(
                listingRepository,
                gigaChatClient,
                properties,
                new ObjectMapper()
        );
    }

    @Test
    void searchesWithValidatedFilterAndMapsListings() {
        when(gigaChatClient.extractListingFilterJson("Need Barnaul hall"))
                .thenReturn("""
                        {
                          "city": "Barnaul",
                          "spaceType": "CONFERENCE_HALL",
                          "minCapacity": 30,
                          "maxPricePerHour": 5000.00,
                          "limit": 5
                        }
                        """);
        when(listingRepository.searchPublished(
                eq(ListingStatus.PUBLISHED),
                eq("Barnaul"),
                eq(SpaceType.CONFERENCE_HALL),
                eq(30),
                argThat(value -> value.compareTo(new BigDecimal("5000.00")) == 0),
                any(Pageable.class)
        )).thenReturn(List.of(listing()));

        var response = service.search(new AiListingSearchRequestDto("  Need Barnaul hall  "));

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("AI room");
        assertThat(response.get(0).ownerOrganizationName()).isEqualTo("Landlord LLC");
        verify(listingRepository).searchPublished(
                eq(ListingStatus.PUBLISHED),
                eq("Barnaul"),
                eq(SpaceType.CONFERENCE_HALL),
                eq(30),
                argThat(value -> value.compareTo(new BigDecimal("5000.00")) == 0),
                eq(Pageable.ofSize(5))
        );
    }

    @Test
    void allowsPartialFiltersAndUsesDefaultLimit() {
        when(gigaChatClient.extractListingFilterJson("Need any loft"))
                .thenReturn("{\"spaceType\":\"LOFT\"}");

        service.search(new AiListingSearchRequestDto("Need any loft"));

        verify(listingRepository).searchPublished(
                eq(ListingStatus.PUBLISHED),
                eq(null),
                eq(SpaceType.LOFT),
                eq(null),
                eq(null),
                eq(Pageable.ofSize(10))
        );
    }

    @Test
    void clampsLimitToConfiguredMaximum() {
        when(gigaChatClient.extractListingFilterJson("Need many rooms"))
                .thenReturn("{\"limit\":99}");

        service.search(new AiListingSearchRequestDto("Need many rooms"));

        verify(listingRepository).searchPublished(
                eq(ListingStatus.PUBLISHED),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(Pageable.ofSize(20))
        );
    }

    @Test
    void rejectsBlankPromptBeforeGigaChatCall() {
        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("prompt must not be blank");

        verify(gigaChatClient, never()).extractListingFilterJson(any());
        verify(listingRepository, never()).searchPublished(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsTooLongPromptBeforeGigaChatCall() {
        properties.setMaxPromptLength(5);

        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto("too long")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("prompt length must be less than or equal to 5");

        verify(gigaChatClient, never()).extractListingFilterJson(any());
    }

    @Test
    void rejectsUnknownSpaceType() {
        when(gigaChatClient.extractListingFilterJson("prompt"))
                .thenReturn("{\"spaceType\":\"UNKNOWN\"}");

        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto("prompt")))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat model returned invalid listing filter");

        verify(listingRepository, never()).searchPublished(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsMalformedJson() {
        when(gigaChatClient.extractListingFilterJson("prompt")).thenReturn("not json");

        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto("prompt")))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat model returned invalid listing filter");
    }

    @Test
    void rejectsUnsupportedExtraFields() {
        when(gigaChatClient.extractListingFilterJson("prompt"))
                .thenReturn("{\"city\":\"Barnaul\",\"sql\":\"DROP TABLE listings\"}");

        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto("prompt")))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat model returned invalid listing filter");
    }

    @Test
    void rejectsNegativeNumericValues() {
        when(gigaChatClient.extractListingFilterJson("prompt"))
                .thenReturn("{\"minCapacity\":-1}");

        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto("prompt")))
                .isInstanceOf(AiListingSearchException.class);
    }

    private GigaChatProperties properties() {
        GigaChatProperties properties = new GigaChatProperties();
        properties.setAuthorizationKey("test-authorization-key");
        properties.setMaxPromptLength(1000);
        properties.setDefaultLimit(10);
        properties.setMaxLimit(20);
        return properties;
    }

    private ListingEntity listing() {
        OrganizationEntity owner = new OrganizationEntity("Landlord LLC", "7700000001");
        ReflectionTestUtils.setField(owner, "id", 17L);
        ListingEntity listing = ListingEntity.published(
                "AI room",
                "Description",
                "Barnaul",
                "Lenina Avenue, 10",
                new BigDecimal("2500.00"),
                40,
                SpaceType.CONFERENCE_HALL,
                "https://example.com/listing.jpg",
                LocalDateTime.of(2026, 7, 14, 10, 0),
                owner
        );
        ReflectionTestUtils.setField(listing, "id", 42L);
        listing.updateCoordinates(new BigDecimal("53.348114"), new BigDecimal("83.779836"));
        return listing;
    }
}
