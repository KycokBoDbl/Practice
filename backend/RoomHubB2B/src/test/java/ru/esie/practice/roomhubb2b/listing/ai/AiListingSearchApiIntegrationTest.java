package ru.esie.practice.roomhubb2b.listing.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.auth.OrganizationRepository;
import ru.esie.practice.roomhubb2b.listing.ListingEntity;
import ru.esie.practice.roomhubb2b.listing.ListingRepository;
import ru.esie.practice.roomhubb2b.listing.SpaceType;
import ru.esie.practice.roomhubb2b.listing.availability.ListingUnavailabilityPeriodEntity;
import ru.esie.practice.roomhubb2b.listing.availability.ListingUnavailabilityPeriodRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "roomhub.booking.scheduler-delay=PT24H")
@AutoConfigureMockMvc
@Transactional
@Import(AiListingSearchApiIntegrationTest.GigaChatTestConfig.class)
class AiListingSearchApiIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(9000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingUnavailabilityPeriodRepository periodRepository;

    @Autowired
    private StubGigaChatCompletionClient gigaChatClient;

    private OrganizationEntity landlord;

    @BeforeEach
    void setUp() {
        landlord = organization("AI search landlord");
        gigaChatClient.response.set("{}");
        gigaChatClient.lastPrompt.set(null);
        gigaChatClient.failure.set(null);
    }

    @Test
    void searchesPublishedListingsFromPromptWithoutAuthentication() throws Exception {
        ListingEntity matching = listing("Matching conference hall", "Барнаул",
                new BigDecimal("4500.00"), 40, SpaceType.CONFERENCE_HALL);
        ListingEntity tooSmall = listing("Small conference hall", "Барнаул",
                new BigDecimal("3500.00"), 10, SpaceType.CONFERENCE_HALL);
        ListingEntity archived = listing("Archived conference hall", "Барнаул",
                new BigDecimal("4000.00"), 40, SpaceType.CONFERENCE_HALL);
        archived.archive();
        listingRepository.saveAndFlush(archived);

        gigaChatClient.response.set("""
                {
                  "city": "Барнаул",
                  "spaceType": "CONFERENCE_HALL",
                  "minCapacity": 30,
                  "maxPricePerHour": 5000.00,
                  "limit": 10
                }
                """);

        mockMvc.perform(post("/api/listings/ai-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Need a conference hall in Barnaul\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.interpretedFilter.city").value("Барнаул"))
                .andExpect(jsonPath("$.interpretedFilter.spaceType").value("CONFERENCE_HALL"))
                .andExpect(jsonPath("$.interpretedFilter.minCapacity").value(30))
                .andExpect(jsonPath("$.interpretedFilter.maxPricePerHour").value(5000.00))
                .andExpect(jsonPath("$.ignoredTerms").isArray())
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].id").value(matching.getId()))
                .andExpect(jsonPath("$.results[0].title").value("Matching conference hall"))
                .andExpect(jsonPath("$.results[0].ownerOrganizationName").value(landlord.getLegalName()))
                .andExpect(jsonPath("$.results[?(@.id == " + tooSmall.getId() + ")]").isEmpty())
                .andExpect(jsonPath("$.results[?(@.id == " + archived.getId() + ")]").isEmpty());

        assertThat(gigaChatClient.lastPrompt.get())
                .contains("Current date:", "Allowed cities:", "Барнаул", "User prompt: Need a conference hall in Barnaul");
    }

    @Test
    void excludesListingsUnavailableForRequestedWindow() throws Exception {
        ListingEntity free = listing("Free room", "Москва",
                new BigDecimal("2500.00"), 10, SpaceType.MEETING_ROOM);
        ListingEntity busy = listing("Busy room", "Москва",
                new BigDecimal("2000.00"), 10, SpaceType.MEETING_ROOM);
        periodRepository.saveAndFlush(new ListingUnavailabilityPeriodEntity(
                busy.getId(),
                LocalDateTime.of(2026, 7, 16, 10, 0),
                LocalDateTime.of(2026, 7, 16, 12, 0)
        ));

        gigaChatClient.response.set("""
                {
                  "city": "Москва",
                  "spaceType": "MEETING_ROOM",
                  "availableFrom": "2026-07-16T09:00",
                  "availableTo": "2026-07-16T11:00",
                  "limit": 10
                }
                """);

        mockMvc.perform(post("/api/listings/ai-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Нужна переговорка в Москве завтра утром\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interpretedFilter.availableFrom").value("2026-07-16T09:00:00"))
                .andExpect(jsonPath("$.interpretedFilter.availableTo").value("2026-07-16T11:00:00"))
                .andExpect(jsonPath("$.results[?(@.id == " + free.getId() + ")]").exists())
                .andExpect(jsonPath("$.results[?(@.id == " + busy.getId() + ")]").isEmpty());
    }

    @Test
    void rejectsBlankPromptBeforeCallingGigaChat() throws Exception {
        mockMvc.perform(post("/api/listings/ai-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        assertThat(gigaChatClient.lastPrompt.get()).isNull();
    }

    @Test
    void returnsEmptyArrayWhenNoListingsMatch() throws Exception {
        listing("Meeting room", "Барнаул", new BigDecimal("2500.00"), 20, SpaceType.MEETING_ROOM);
        gigaChatClient.response.set("{\"city\":\"Барнаул\",\"minCapacity\":100}");

        mockMvc.perform(post("/api/listings/ai-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Need a large room in Barnaul\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(0));
    }

    @Test
    void mapsGigaChatFailureToBadGatewayProblem() throws Exception {
        gigaChatClient.failure.set(new AiListingSearchException("GigaChat model request failed"));

        mockMvc.perform(post("/api/listings/ai-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Need a room\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail").value("GigaChat model request failed"));
    }

    @Test
    void existingListingCatalogRemainsPublic() throws Exception {
        ListingEntity listing = listing("Existing catalog room", "Барнаул",
                new BigDecimal("2500.00"), 20, SpaceType.MEETING_ROOM);

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + listing.getId() + ")].title")
                        .value("Existing catalog room"));
    }

    private ListingEntity listing(
            String title,
            String city,
            BigDecimal pricePerHour,
            int capacity,
            SpaceType spaceType
    ) {
        ListingEntity listing = ListingEntity.published(
                title,
                "AI searchable room",
                city,
                "Lenina Avenue, 10",
                pricePerHour,
                capacity,
                spaceType,
                "https://example.com/listing.jpg",
                LocalDateTime.of(2026, 7, 14, 10, 0),
                landlord
        );
        listing.updateCoordinates(new BigDecimal("53.348114"), new BigDecimal("83.779836"));
        return listingRepository.saveAndFlush(listing);
    }

    private OrganizationEntity organization(String label) {
        int number = SEQUENCE.incrementAndGet();
        return organizationRepository.saveAndFlush(new OrganizationEntity(
                label + " " + number,
                String.format("77%08d", number)
        ));
    }

    @TestConfiguration
    static class GigaChatTestConfig {

        @Bean
        @Primary
        StubGigaChatCompletionClient gigaChatCompletionClient() {
            return new StubGigaChatCompletionClient();
        }
    }

    static class StubGigaChatCompletionClient implements GigaChatCompletionClient {

        private final AtomicReference<String> response = new AtomicReference<>("{}");
        private final AtomicReference<String> lastPrompt = new AtomicReference<>();
        private final AtomicReference<RuntimeException> failure = new AtomicReference<>();

        @Override
        public String extractListingFilterJson(String prompt) {
            lastPrompt.set(prompt);
            RuntimeException exception = failure.getAndSet(null);
            if (exception != null) {
                throw exception;
            }
            return response.get();
        }
    }
}
