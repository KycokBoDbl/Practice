package ru.esie.practice.roomhubb2b.auth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.esie.practice.roomhubb2b.config.TokenProperties;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AuthApiIntegrationTest.RoleProtectedTestController.class)
class AuthApiIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(1000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private TokenProperties tokenProperties;

    @Autowired
    private Clock clock;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registersLogsInAndReturnsCurrentProfileWithoutSession() throws Exception {
        Registration registration = nextRegistration("LANDLORD");
        MvcResult registerResult = register(registration)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("LANDLORD"))
                .andExpect(jsonPath("$.legalName").value(registration.legalName()))
                .andExpect(jsonPath("$.taxId").value(registration.taxId()))
                .andExpect(jsonPath("$.email").value(registration.email()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        JsonNode profile = objectMapper.readTree(registerResult.getResponse().getContentAsString());

        String token = login(registration.email().toUpperCase(), registration.password())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        token = objectMapper.readTree(token).get("accessToken").asText();

        org.springframework.security.oauth2.jwt.Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(profile.get("userId").asText());
        assertThat(((Number) jwt.getClaim("organizationId")).longValue())
                .isEqualTo(profile.get("organizationId").asLong());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("LANDLORD");
        assertThat(jwt.getClaims()).doesNotContainKeys("password", "passwordHash", "legalName", "taxId", "email");

        MvcResult meResult = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(profile.get("userId").asLong()))
                .andExpect(jsonPath("$.organizationId").value(profile.get("organizationId").asLong()))
                .andExpect(jsonPath("$.taxId").value(registration.taxId()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        assertThat(meResult.getRequest().getSession(false)).isNull();
    }

    @Test
    void rejectsInvalidRegistrationAndDuplicateIdentifiers() throws Exception {
        Registration registration = nextRegistration("TENANT");
        register(registration).andExpect(status().isCreated());
        long organizations = organizationRepository.count();
        long users = userRepository.count();

        register(new Registration(
                "TENANT",
                "ООО Duplicate Email",
                nextTaxId(),
                " " + registration.email().toUpperCase() + " ",
                registration.password()
        ))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
        assertThat(organizationRepository.count()).isEqualTo(organizations);
        assertThat(userRepository.count()).isEqualTo(users);

        register(new Registration(
                "TENANT",
                "ООО Invalid Tax",
                "123456789012",
                "invalid-tax@example.com",
                registration.password()
        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[?(@.field == 'taxId')]").exists());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN",
                                  "legalName": "ООО Invalid Role",
                                  "taxId": "1234567890",
                                  "email": "invalid-role@example.com",
                                  "password": "S3cure-roomhub-password"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void doesNotRevealWhetherEmailExistsDuringLogin() throws Exception {
        Registration registration = nextRegistration("TENANT");
        register(registration).andExpect(status().isCreated());

        MvcResult wrongPassword = login(registration.email(), "wrong-password-value")
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult unknownEmail = login("unknown@example.com", "wrong-password-value")
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(objectMapper.readTree(wrongPassword.getResponse().getContentAsString()).get("detail").asText())
                .isEqualTo(objectMapper.readTree(unknownEmail.getResponse().getContentAsString()).get("detail").asText())
                .isEqualTo("Invalid email or password");
    }

    @Test
    void enforcesPublicAllowlistAndRole() throws Exception {
        MvcResult listingsResult = mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andReturn();
        long listingId = objectMapper.readTree(listingsResult.getResponse().getContentAsString()).get(0).get("id").asLong();
        mockMvc.perform(get("/api/listings/{listingId}/availability", listingId)
                        .queryParam("from", "2026-07-01T09:00")
                        .queryParam("to", "2026-07-01T18:00"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/openapi"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/private-probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        Registration tenant = nextRegistration("TENANT");
        register(tenant).andExpect(status().isCreated());
        String token = accessToken(login(tenant.email(), tenant.password()).andReturn());
        mockMvc.perform(get("/api/test/landlord").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void rejectsMissingExpiredWrongIssuerAndTamperedTokens() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        String expired = customToken("1", tokenProperties.issuer(), clock.instant().minusSeconds(600), clock.instant().minusSeconds(300));
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());

        String wrongIssuer = customToken("1", "another-issuer", clock.instant(), clock.instant().plusSeconds(300));
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + wrongIssuer))
                .andExpect(status().isUnauthorized());

        Registration registration = nextRegistration("LANDLORD");
        register(registration).andExpect(status().isCreated());
        String valid = accessToken(login(registration.email(), registration.password()).andReturn());
        char replacement = valid.endsWith("A") ? 'B' : 'A';
        String tampered = valid.substring(0, valid.length() - 1) + replacement;
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());

        String missingAccount = customToken(
                Long.toString(Long.MAX_VALUE),
                tokenProperties.issuer(),
                clock.instant(),
                clock.instant().plusSeconds(300)
        );
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + missingAccount))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions register(Registration registration) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(registration)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new Login(email, password))));
    }

    private String accessToken(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String customToken(String subject, String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("organizationId", 1L)
                .claim("role", "LANDLORD")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
    }

    private static Registration nextRegistration(String role) {
        int number = SEQUENCE.incrementAndGet();
        return new Registration(
                role,
                "ООО Integration " + number,
                String.format("73%08d", number),
                "account-" + number + "@example.com",
                "S3cure-roomhub-password"
        );
    }

    private static String nextTaxId() {
        return String.format("74%08d", SEQUENCE.incrementAndGet());
    }

    private record Registration(String role, String legalName, String taxId, String email, String password) {
    }

    private record Login(String email, String password) {
    }

    @RestController
    static class RoleProtectedTestController {

        @GetMapping("/api/test/landlord")
        @PreAuthorize("hasRole('LANDLORD')")
        String landlordOnly() {
            return "ok";
        }
    }
}
