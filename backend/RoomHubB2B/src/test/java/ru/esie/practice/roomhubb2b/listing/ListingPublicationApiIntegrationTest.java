package ru.esie.practice.roomhubb2b.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.auth.OrganizationRepository;
import ru.esie.practice.roomhubb2b.auth.UserRole;
import ru.esie.practice.roomhubb2b.booking.BookingEntity;
import ru.esie.practice.roomhubb2b.booking.BookingRepository;
import ru.esie.practice.roomhubb2b.config.TokenProperties;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "roomhub.booking.scheduler-delay=PT24H")
@AutoConfigureMockMvc
@Transactional
class ListingPublicationApiIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(5000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private TokenProperties tokenProperties;

    @Autowired
    private Clock clock;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private OrganizationEntity landlord;
    private OrganizationEntity tenant;
    private OrganizationEntity otherLandlord;
    private String landlordToken;
    private String tenantToken;
    private String otherLandlordToken;

    @BeforeEach
    void setUp() {
        landlord = organization("Publication landlord");
        tenant = organization("Publication tenant");
        otherLandlord = organization("Publication other landlord");
        landlordToken = token(landlord.getId(), UserRole.LANDLORD);
        tenantToken = token(tenant.getId(), UserRole.TENANT);
        otherLandlordToken = token(otherLandlord.getId(), UserRole.LANDLORD);
    }

    @Test
    void publishesServerOwnedListingAndMakesItPublic() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(tenant.getId())))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$.title").value("Publication meeting room"))
                .andExpect(jsonPath("$.pricePerHour").value(2500.00))
                .andExpect(jsonPath("$.spaceType").value("MEETING_ROOM"))
                .andExpect(jsonPath("$.ownerOrganizationName").value(landlord.getLegalName()))
                .andExpect(jsonPath("$.ownerOrganizationId").doesNotExist())
                .andReturn();

        long listingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/listings/" + listingId);
        ListingEntity listing = listingRepository.findById(listingId).orElseThrow();
        assertThat(listing.getOwnerOrganization().getId()).isEqualTo(landlord.getId());
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(listing.getCreatedAt()).isNotNull();

        Map<String, Object> stored = jdbcTemplate.queryForMap(
                "SELECT owner_organization_id, status, created_at FROM listings WHERE id = ?",
                listingId
        );
        assertThat(stored.get("owner_organization_id")).isEqualTo(landlord.getId());
        assertThat(stored.get("status")).isEqualTo("PUBLISHED");
        assertThat(stored.get("created_at")).isNotNull();

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + listingId + ")].title")
                        .value("Publication meeting room"));
    }

    @Test
    void acceptsMissingOptionalFields() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Minimal listing",
                                  "city": "Barnaul",
                                  "address": "Lenina Avenue, 10",
                                  "pricePerHour": 1.00,
                                  "capacity": 1,
                                  "spaceType": "CLASSROOM"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.imageUrl").isEmpty());
    }

    @Test
    void rejectsUnauthenticatedAndTenantRequestsWithoutInserting() throws Exception {
        long count = listingRepository.count();

        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(tenant.getId())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(landlord.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        assertThat(listingRepository.count()).isEqualTo(count);
    }

    @Test
    void rejectsBeanValidationErrorsWithoutInserting() throws Exception {
        long count = listingRepository.count();

        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "city": "Barnaul",
                                  "address": "Lenina Avenue, 10",
                                  "pricePerHour": 0,
                                  "capacity": 0,
                                  "spaceType": "MEETING_ROOM",
                                  "imageUrl": "ftp://example.com/image.jpg"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'pricePerHour')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'capacity')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'imageUrl')]").exists());

        assertThat(listingRepository.count()).isEqualTo(count);
    }

    @Test
    void returnsFieldErrorForUnknownSpaceTypeAndGenericErrorForMalformedJson() throws Exception {
        long count = listingRepository.count();

        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Meeting room",
                                  "city": "Barnaul",
                                  "address": "Lenina Avenue, 10",
                                  "pricePerHour": 2500.00,
                                  "capacity": 20,
                                  "spaceType": "UNKNOWN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("spaceType"))
                .andExpect(jsonPath("$.errors[0].message").value("unsupported enum value"));

        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").doesNotExist());

        assertThat(listingRepository.count()).isEqualTo(count);
    }

    @Test
    void updatesOwnedListingAndPreservesServerControlledFields() throws Exception {
        ListingEntity listing = listing("Original room", landlord);
        LocalDateTime createdAt = listing.getCreatedAt();

        mockMvc.perform(put("/api/listings/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest(otherLandlord.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Updated management room"))
                .andExpect(jsonPath("$.city").value("Novosibirsk"))
                .andExpect(jsonPath("$.ownerOrganizationName").value(landlord.getLegalName()))
                .andExpect(jsonPath("$.ownerOrganizationId").doesNotExist());

        ListingEntity updated = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated management room");
        assertThat(updated.getDescription()).isNull();
        assertThat(updated.getCity()).isEqualTo("Novosibirsk");
        assertThat(updated.getAddress()).isEqualTo("Krasny Avenue, 1");
        assertThat(updated.getPricePerHour()).isEqualByComparingTo("3000.00");
        assertThat(updated.getCapacity()).isEqualTo(24);
        assertThat(updated.getSpaceType()).isEqualTo(SpaceType.CONFERENCE_HALL);
        assertThat(updated.getImageUrl()).isNull();
        assertThat(updated.getOwnerOrganization().getId()).isEqualTo(landlord.getId());
        assertThat(updated.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void listsOwnedPublishedAndHiddenListingsForLandlord() throws Exception {
        ListingEntity published = listing("Owned visible room", landlord);
        ListingEntity hidden = listing("Owned hidden room", landlord);
        hidden.archive();
        listingRepository.saveAndFlush(hidden);
        ListingEntity other = listing("Other landlord room", otherLandlord);

        mockMvc.perform(get("/api/listings/owned")
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == " + published.getId() + ")].title")
                        .value("Owned visible room"))
                .andExpect(jsonPath("$[?(@.id == " + published.getId() + ")].status")
                        .value("PUBLISHED"))
                .andExpect(jsonPath("$[?(@.id == " + hidden.getId() + ")].title")
                        .value("Owned hidden room"))
                .andExpect(jsonPath("$[?(@.id == " + hidden.getId() + ")].status")
                        .value("ARCHIVED"))
                .andExpect(jsonPath("$[?(@.id == " + hidden.getId() + ")].ownerOrganizationName")
                        .value(landlord.getLegalName()))
                .andExpect(jsonPath("$[?(@.id == " + hidden.getId() + ")].ownerOrganizationId")
                        .doesNotExist())
                .andExpect(jsonPath("$[?(@.id == " + other.getId() + ")]").isEmpty());

        mockMvc.perform(get("/api/listings/owned")
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/listings/owned"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidUpdateWithoutChangingListing() throws Exception {
        ListingEntity listing = listing("Original valid room", landlord);

        mockMvc.perform(put("/api/listings/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "city": "Novosibirsk",
                                  "address": "Krasny Avenue, 1",
                                  "pricePerHour": 0,
                                  "capacity": 0,
                                  "spaceType": "CONFERENCE_HALL",
                                  "imageUrl": "ftp://example.com/image.jpg"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'pricePerHour')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'capacity')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'imageUrl')]").exists());

        ListingEntity unchanged = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Original valid room");
        assertThat(unchanged.getCity()).isEqualTo("Barnaul");
        assertThat(unchanged.getAddress()).isEqualTo("Lenina Avenue, 10");
        assertThat(unchanged.getPricePerHour()).isEqualByComparingTo("2500.00");
        assertThat(unchanged.getCapacity()).isEqualTo(20);
        assertThat(unchanged.getImageUrl()).isEqualTo("https://example.com/listing.jpg");
    }

    @Test
    void rejectsUnauthenticatedTenantAndNonOwnerManagementRequests() throws Exception {
        ListingEntity listing = listing("Protected room", landlord);

        mockMvc.perform(delete("/api/listings/{listingId}", listing.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post("/api/listings/{listingId}/hide", listing.getId())
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(put("/api/listings/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + otherLandlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest(otherLandlord.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        ListingEntity unchanged = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Protected room");
        assertThat(unchanged.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
    }

    @Test
    void hidesAndReactivatesOwnedListingInPublicCatalog() throws Exception {
        ListingEntity listing = listing("Managed visibility room", landlord);
        long bookingCount = bookingRepository.count();

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + listing.getId() + ")].title")
                        .value("Managed visibility room"));

        mockMvc.perform(post("/api/listings/{listingId}/hide", listing.getId())
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/listings/{listingId}/hide", listing.getId())
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isNoContent());

        assertThat(listingRepository.findById(listing.getId()).orElseThrow().getStatus())
                .isEqualTo(ListingStatus.ARCHIVED);
        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + listing.getId() + ")]").isEmpty());
        mockMvc.perform(get("/api/listings/{listingId}/availability", listing.getId())
                        .param("from", "2030-01-01T09:00")
                        .param("to", "2030-01-01T18:00"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(listing.getId())))
                .andExpect(status().isNotFound());
        assertThat(bookingRepository.count()).isEqualTo(bookingCount);

        mockMvc.perform(post("/api/listings/{listingId}/activate", listing.getId())
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/listings/{listingId}/activate", listing.getId())
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isNoContent());

        assertThat(listingRepository.findById(listing.getId()).orElseThrow().getStatus())
                .isEqualTo(ListingStatus.PUBLISHED);
        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + listing.getId() + ")].title")
                        .value("Managed visibility room"));
        mockMvc.perform(get("/api/listings/{listingId}/availability", listing.getId())
                        .param("from", "2030-01-01T09:00")
                        .param("to", "2030-01-01T18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingId").value(listing.getId()));
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest(listing.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value(listing.getId()));
        assertThat(bookingRepository.count()).isEqualTo(bookingCount + 1);
    }

    @Test
    void deletesOwnedListingWithoutBookingsAndCascadesManualAvailabilityPeriods() throws Exception {
        ListingEntity listing = listing("Disposable room", landlord);
        jdbcTemplate.update(
                """
                        INSERT INTO listing_unavailability_periods (listing_id, start_at, end_at)
                        VALUES (?, ?, ?)
                        """,
                listing.getId(),
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 1, 11, 0)
        );

        mockMvc.perform(delete("/api/listings/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isNoContent());
        listingRepository.flush();

        assertThat(listingRepository.findById(listing.getId())).isEmpty();
        Long periodCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM listing_unavailability_periods WHERE listing_id = ?",
                Long.class,
                listing.getId()
        );
        assertThat(periodCount).isZero();
    }

    @Test
    void rejectsDeletingListingWithBookingHistory() throws Exception {
        ListingEntity listing = listing("Booked room", landlord);
        bookingRepository.saveAndFlush(new BookingEntity(
                listing,
                tenant,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 1, 12, 0),
                new BigDecimal("2500.00"),
                new BigDecimal("5000.00"),
                LocalDateTime.of(2026, 7, 8, 10, 0)
        ));

        mockMvc.perform(delete("/api/listings/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(listingRepository.findById(listing.getId())).isPresent();
    }

    private String validRequest(Long suppliedOwnerId) {
        return """
                {
                  "title": "Publication meeting room",
                  "description": "Screen and flip chart",
                  "city": "Barnaul",
                  "address": "Lenina Avenue, 10",
                  "pricePerHour": 2500.00,
                  "capacity": 20,
                  "spaceType": "MEETING_ROOM",
                  "imageUrl": "https://example.com/listing.jpg",
                  "ownerOrganizationId": %d,
                  "status": "ARCHIVED",
                  "createdAt": "2000-01-01T00:00:00"
                }
                """.formatted(suppliedOwnerId);
    }

    private String updateRequest(Long suppliedOwnerId) {
        return """
                {
                  "title": "Updated management room",
                  "description": null,
                  "city": "Novosibirsk",
                  "address": "Krasny Avenue, 1",
                  "pricePerHour": 3000.00,
                  "capacity": 24,
                  "spaceType": "CONFERENCE_HALL",
                  "imageUrl": null,
                  "ownerOrganizationId": %d,
                  "status": "ARCHIVED",
                  "createdAt": "2000-01-01T00:00:00"
                }
                """.formatted(suppliedOwnerId);
    }

    private String bookingRequest(Long listingId) {
        return """
                {
                  "listingId": %d,
                  "startAt": "2030-01-01T10:00",
                  "endAt": "2030-01-01T12:00"
                }
                """.formatted(listingId);
    }

    private ListingEntity listing(String title, OrganizationEntity owner) {
        return listingRepository.saveAndFlush(ListingEntity.published(
                title,
                "Projector and whiteboard",
                "Barnaul",
                "Lenina Avenue, 10",
                new BigDecimal("2500.00"),
                20,
                SpaceType.MEETING_ROOM,
                "https://example.com/listing.jpg",
                LocalDateTime.of(2026, 7, 8, 10, 0),
                owner
        ));
    }

    private OrganizationEntity organization(String label) {
        int number = SEQUENCE.incrementAndGet();
        return organizationRepository.saveAndFlush(new OrganizationEntity(
                label + " " + number,
                String.format("78%08d", number)
        ));
    }

    private String token(Long organizationId, UserRole role) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(Long.toString(organizationId))
                .issuer(tokenProperties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(600))
                .claim("organizationId", organizationId)
                .claim("role", role.name())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
    }
}
