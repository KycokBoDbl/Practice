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
import ru.esie.practice.roomhubb2b.config.TokenProperties;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private JdbcTemplate jdbcTemplate;

    private OrganizationEntity landlord;
    private OrganizationEntity tenant;
    private String landlordToken;
    private String tenantToken;

    @BeforeEach
    void setUp() {
        landlord = organization("Publication landlord");
        tenant = organization("Publication tenant");
        landlordToken = token(landlord.getId(), UserRole.LANDLORD);
        tenantToken = token(tenant.getId(), UserRole.TENANT);
    }

    @Test
    void publishesServerOwnedListingAndMakesItPublic() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(tenant.getId())))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$.title").value("Publication meeting room"))
                .andExpect(jsonPath("$.pricePerHour").value(2500.00))
                .andExpect(jsonPath("$.spaceType").value("MEETING_ROOM"))
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
