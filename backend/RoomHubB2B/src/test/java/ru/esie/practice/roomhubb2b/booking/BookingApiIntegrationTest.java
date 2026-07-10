package ru.esie.practice.roomhubb2b.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
import ru.esie.practice.roomhubb2b.listing.ListingEntity;
import ru.esie.practice.roomhubb2b.listing.ListingRepository;
import ru.esie.practice.roomhubb2b.listing.ListingStatus;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "roomhub.booking.scheduler-delay=PT24H")
@AutoConfigureMockMvc
@Transactional
class BookingApiIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(3000);

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

    private OrganizationEntity landlord;
    private OrganizationEntity tenant;
    private OrganizationEntity outsider;
    private Long listingId;
    private String landlordToken;
    private String tenantToken;
    private String outsiderToken;

    @BeforeEach
    void setUp() {
        landlord = organization("Landlord");
        tenant = organization("Tenant");
        outsider = organization("Outsider");
        ListingEntity listing = listingRepository.findByStatus(ListingStatus.PUBLISHED).get(0);
        listing.assignOwner(landlord);
        listingId = listingRepository.saveAndFlush(listing).getId();
        landlordToken = token(landlord.getId(), UserRole.LANDLORD);
        tenantToken = token(tenant.getId(), UserRole.TENANT);
        outsiderToken = token(outsider.getId(), UserRole.TENANT);
    }

    @Test
    void protectsBookingEndpointsAndIgnoresClientParticipantIds() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(tenant.getId())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(outsider.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(outsider.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$.listingId").value(listingId))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.startAt").value("2035-01-01T10:00"))
                .andExpect(jsonPath("$.totalPrice").isNumber())
                .andReturn();

        long bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getTenantOrganization().getId())
                .isEqualTo(tenant.getId());

        mockMvc.perform(get("/api/bookings/{id}", bookingId)
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/bookings/{id}", bookingId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void enforcesCommandRolesAndRunsAuthenticatedFlow() throws Exception {
        long bookingId = createBooking();

        mockMvc.perform(post("/api/bookings/{id}/approve", bookingId)
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/bookings/{id}/approve", bookingId)
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"));
        mockMvc.perform(post("/api/bookings/{id}/confirm", bookingId)
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/bookings/{id}/confirm", bookingId)
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(get("/api/bookings/{id}/history", bookingId)
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(patch("/api/bookings/{id}", bookingId)
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void returnsBadRequestForMalformedHourlyRequest() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "listingId": %d,
                                  "startAt": "2035-01-01T10:01",
                                  "endAt": "2035-01-01T11:00"
                                }
                                """.formatted(listingId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsConflictProblemForDuplicateBookingApplication() throws Exception {
        createBooking("2035-01-03T10:00", "2035-01-03T11:00");

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(tenant.getId(), "2035-01-03T12:00", "2035-01-03T13:00")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail")
                        .value("Tenant already has a booking request for this listing on this date"));

        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void exposesParticipantInboxWithVisibilityFilteringAndStableShape() throws Exception {
        long requestedBookingId = createBooking("2035-01-01T10:00", "2035-01-01T13:00");
        long confirmedBookingId = createBooking("2035-01-02T10:00", "2035-01-02T13:00");

        mockMvc.perform(post("/api/bookings/{id}/approve", confirmedBookingId)
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/bookings/{id}/confirm", confirmedBookingId)
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(confirmedBookingId))
                .andExpect(jsonPath("$[0].listingId").value(listingId))
                .andExpect(jsonPath("$[0].listingTitle").isNotEmpty())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].startAt").value("2035-01-02T10:00"))
                .andExpect(jsonPath("$[0].endAt").value("2035-01-02T13:00"))
                .andExpect(jsonPath("$[0].pricePerHour").isNumber())
                .andExpect(jsonPath("$[0].totalPrice").isNumber())
                .andExpect(jsonPath("$[0].tenantOrganizationName").value(tenant.getLegalName()))
                .andExpect(jsonPath("$[0].landlordOrganizationName").value(landlord.getLegalName()))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[0].updatedAt").isNotEmpty())
                .andExpect(jsonPath("$[1].id").value(requestedBookingId))
                .andExpect(jsonPath("$[1].status").value("REQUESTED"));

        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(confirmedBookingId))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void keepsInboxIsolatedFromOtherOrganizationsAndRequiresAuthentication() throws Exception {
        long bookingId = createBooking();

        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/bookings/{id}", bookingId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private long createBooking() throws Exception {
        return createBooking("2035-01-01T10:00", "2035-01-01T13:00");
    }

    private long createBooking(String startAt, String endAt) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(tenant.getId(), startAt, endAt)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String requestJson(Long suppliedTenantId) {
        return requestJson(suppliedTenantId, "2035-01-01T10:00", "2035-01-01T13:00");
    }

    private String requestJson(Long suppliedTenantId, String startAt, String endAt) {
        return """
                {
                  "listingId": %d,
                  "startAt": "%s",
                  "endAt": "%s",
                  "tenantOrganizationId": %d
                }
                """.formatted(listingId, startAt, endAt, suppliedTenantId);
    }

    private OrganizationEntity organization(String label) {
        int number = SEQUENCE.incrementAndGet();
        return organizationRepository.saveAndFlush(new OrganizationEntity(
                label + " " + number,
                String.format("77%08d", number)
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
