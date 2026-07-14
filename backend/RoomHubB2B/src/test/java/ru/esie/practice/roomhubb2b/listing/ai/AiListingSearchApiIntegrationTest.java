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
        ListingEntity matching = listing("Matching conference hall", "Barnaul",
                new BigDecimal("4500.00"), 40, SpaceType.CONFERENCE_HALL);
        ListingEntity tooSmall = listing("Small conference hall", "Barnaul",
                new BigDecimal("3500.00"), 10, SpaceType.CONFERENCE_HALL);
        ListingEntity archived = listing("Archived conference hall", "Barnaul",
                new BigDecimal("4000.00"), 40, SpaceType.CONFERENCE_HALL);
        archived.archive();
        listingRepository.saveAndFlush(archived);

        gigaChatClient.response.set("""
                {
                  "city": "Barnaul",
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
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(matching.getId()))
                .andExpect(jsonPath("$[0].title").value("Matching conference hall"))
                .andExpect(jsonPath("$[0].ownerOrganizationName").value(landlord.getLegalName()))
                .andExpect(jsonPath("$[?(@.id == " + tooSmall.getId() + ")]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == " + archived.getId() + ")]").isEmpty());

        assertThat(gigaChatClient.lastPrompt.get()).isEqualTo("Need a conference hall in Barnaul");
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
        listing("Meeting room", "Barnaul", new BigDecimal("2500.00"), 20, SpaceType.MEETING_ROOM);
        gigaChatClient.response.set("{\"city\":\"Novosibirsk\"}");

        mockMvc.perform(post("/api/listings/ai-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Need something in Novosibirsk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
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
        ListingEntity listing = listing("Existing catalog room", "Barnaul",
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
