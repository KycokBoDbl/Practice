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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiListingSearchServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC);

    private ListingRepository listingRepository;
    private GigaChatCompletionClient gigaChatClient;
    private GigaChatProperties properties;
    private AiListingSearchService service;

    @BeforeEach
    void setUp() {
        listingRepository = mock(ListingRepository.class);
        gigaChatClient = mock(GigaChatCompletionClient.class);
        properties = properties();
        when(listingRepository.findDistinctCitiesByStatus(ListingStatus.PUBLISHED))
                .thenReturn(List.of("Барнаул", "Москва"));
        service = new AiListingSearchService(
                listingRepository,
                gigaChatClient,
                properties,
                new ObjectMapper(),
                CLOCK
        );
    }

    @Test
    void searchesWithValidatedFilterAndMapsListings() {
        when(gigaChatClient.extractListingFilterJson(any()))
                .thenReturn("""
                        {
                          "city": "Барнаул",
                          "spaceType": "CONFERENCE_HALL",
                          "minCapacity": 30,
                          "minPricePerHour": 1000.00,
                          "maxPricePerHour": 5000.00,
                          "availableFrom": "2026-07-16T09:00",
                          "availableTo": "2026-07-16T18:00",
                          "ignoredTerms": ["с проектором"],
                          "limit": 5
                        }
                        """);
        when(listingRepository.searchPublished(
                eq(ListingStatus.PUBLISHED),
                eq("барнаул"),
                eq(SpaceType.CONFERENCE_HALL),
                eq(30),
                argThat(value -> value.compareTo(new BigDecimal("1000.00")) == 0),
                argThat(value -> value.compareTo(new BigDecimal("5000.00")) == 0),
                eq(true),
                eq(LocalDateTime.of(2026, 7, 16, 9, 0)),
                eq(LocalDateTime.of(2026, 7, 16, 18, 0)),
                any(Pageable.class)
        )).thenReturn(List.of(listing()));

        var response = service.search(new AiListingSearchRequestDto("  Need Barnaul hall  "));

        assertThat(response.results()).hasSize(1);
        assertThat(response.interpretedFilter().city()).isEqualTo("Барнаул");
        assertThat(response.interpretedFilter().minPricePerHour()).isEqualByComparingTo("1000.00");
        assertThat(response.interpretedFilter().availableFrom()).isEqualTo(LocalDateTime.of(2026, 7, 16, 9, 0));
        assertThat(response.ignoredTerms()).containsExactly("с проектором");
        assertThat(response.results().get(0).title()).isEqualTo("AI room");
        assertThat(response.results().get(0).ownerOrganizationName()).isEqualTo("Landlord LLC");
        verify(gigaChatClient).extractListingFilterJson(argThat(prompt ->
                prompt.contains("Allowed cities: [\"Барнаул\",\"Москва\"]")
                        && prompt.contains("Allowed space types: [\"MEETING_ROOM\",\"CONFERENCE_HALL\",\"CLASSROOM\",\"LOFT\",\"SHOWROOM\"]")
                        && prompt.contains("User prompt: Need Barnaul hall")
        ));
        verify(listingRepository).searchPublished(
                eq(ListingStatus.PUBLISHED),
                eq("барнаул"),
                eq(SpaceType.CONFERENCE_HALL),
                eq(30),
                argThat(value -> value.compareTo(new BigDecimal("1000.00")) == 0),
                argThat(value -> value.compareTo(new BigDecimal("5000.00")) == 0),
                eq(true),
                eq(LocalDateTime.of(2026, 7, 16, 9, 0)),
                eq(LocalDateTime.of(2026, 7, 16, 18, 0)),
                eq(Pageable.ofSize(5))
        );
    }

    @Test
    void allowsPartialFiltersAndUsesDefaultLimit() {
        when(gigaChatClient.extractListingFilterJson(any()))
                .thenReturn("{\"spaceType\":\"LOFT\"}");

        service.search(new AiListingSearchRequestDto("Need any loft"));

        verify(listingRepository).searchPublished(
                eq(ListingStatus.PUBLISHED),
                eq(null),
                eq(SpaceType.LOFT),
                eq(null),
                eq(null),
                eq(null),
                eq(false),
                eq(LocalDateTime.of(1, 1, 1, 0, 0)),
                eq(LocalDateTime.of(9999, 12, 31, 0, 0)),
                eq(Pageable.ofSize(10))
        );
    }

    @Test
    void clampsLimitToConfiguredMaximum() {
        when(gigaChatClient.extractListingFilterJson(any()))
                .thenReturn("{\"limit\":99}");

        service.search(new AiListingSearchRequestDto("Need many rooms"));

        verify(listingRepository).searchPublished(
                eq(ListingStatus.PUBLISHED),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(false),
                eq(LocalDateTime.of(1, 1, 1, 0, 0)),
                eq(LocalDateTime.of(9999, 12, 31, 0, 0)),
                eq(Pageable.ofSize(20))
        );
    }

    @Test
    void resolvesRelativeAvailabilityDates() {
        when(gigaChatClient.extractListingFilterJson(any()))
                .thenReturn("""
                        {
                          "city": "Москва",
                          "availableFrom": "TOMORROW_START",
                          "availableTo": "TOMORROW_END"
                        }
                        """);

        AiListingSearchResponseDto response = service.search(new AiListingSearchRequestDto("на завтра в мск"));

        assertThat(response.interpretedFilter().city()).isEqualTo("Москва");
        assertThat(response.interpretedFilter().availableFrom()).isEqualTo(LocalDateTime.of(2026, 7, 16, 0, 0));
        assertThat(response.interpretedFilter().availableTo()).isEqualTo(LocalDateTime.of(2026, 7, 17, 0, 0));
        verify(listingRepository).searchPublished(
                eq(ListingStatus.PUBLISHED),
                eq("москва"),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(true),
                eq(LocalDateTime.of(2026, 7, 16, 0, 0)),
                eq(LocalDateTime.of(2026, 7, 17, 0, 0)),
                eq(Pageable.ofSize(10))
        );
    }

    @Test
    void rejectsCityOutsideAllowedList() {
        when(gigaChatClient.extractListingFilterJson(any()))
                .thenReturn("{\"city\":\"Novosibirsk\"}");

        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto("prompt")))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat model returned invalid listing filter");

        verify(listingRepository, never()).searchPublished(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any()
        );
    }

    @Test
    void rejectsBlankPromptBeforeGigaChatCall() {
        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("prompt must not be blank");

        verify(gigaChatClient, never()).extractListingFilterJson(any());
        verify(listingRepository, never()).searchPublished(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any()
        );
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
        when(gigaChatClient.extractListingFilterJson(any()))
                .thenReturn("{\"spaceType\":\"UNKNOWN\"}");

        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto("prompt")))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat model returned invalid listing filter");

        verify(listingRepository, never()).searchPublished(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any()
        );
    }

    @Test
    void rejectsMalformedJson() {
        when(gigaChatClient.extractListingFilterJson(any())).thenReturn("not json");

        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto("prompt")))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat model returned invalid listing filter");
    }

    @Test
    void rejectsUnsupportedExtraFields() {
        when(gigaChatClient.extractListingFilterJson(any()))
                .thenReturn("{\"city\":\"Barnaul\",\"sql\":\"DROP TABLE listings\"}");

        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto("prompt")))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat model returned invalid listing filter");
    }

    @Test
    void rejectsNegativeNumericValues() {
        when(gigaChatClient.extractListingFilterJson(any()))
                .thenReturn("{\"minCapacity\":-1}");

        assertThatThrownBy(() -> service.search(new AiListingSearchRequestDto("prompt")))
                .isInstanceOf(AiListingSearchException.class);
    }

    @Test
    void rejectsInvalidPriceRange() {
        when(gigaChatClient.extractListingFilterJson(any()))
                .thenReturn("{\"minPricePerHour\":5000,\"maxPricePerHour\":1000}");

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
