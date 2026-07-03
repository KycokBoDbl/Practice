package ru.esie.practice.roomhubb2b.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import ru.esie.practice.roomhubb2b.auth.dto.TokenResponseDto;
import ru.esie.practice.roomhubb2b.config.TokenProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessTokenServiceTest {

    @Test
    void issuesExpectedClaimsUsingInjectedClock() {
        Instant now = Instant.parse("2026-07-02T08:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        TokenProperties properties = new TokenProperties(
                "roomhub-test",
                Duration.ofMinutes(15),
                "12345678901234567890123456789012"
        );
        SecretKey key = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withSecretKey(key)
                .algorithm(MacAlgorithm.HS256)
                .build();
        AccessTokenService service = new AccessTokenService(encoder, properties, clock);
        OrganizationEntity organization = mock(OrganizationEntity.class);
        UserEntity user = mock(UserEntity.class);
        when(organization.getId()).thenReturn(7L);
        when(user.getId()).thenReturn(12L);
        when(user.getOrganization()).thenReturn(organization);
        when(user.getRole()).thenReturn(UserRole.LANDLORD);

        TokenResponseDto response = service.issue(user);

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(clock);
        decoder.setJwtValidator(timestampValidator);
        Jwt jwt = decoder.decode(response.accessToken());
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("roomhub-test");
        assertThat(jwt.getSubject()).isEqualTo("12");
        assertThat(((Number) jwt.getClaim("organizationId")).longValue()).isEqualTo(7L);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("LANDLORD");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plusSeconds(900));
        assertThat(jwt.getClaims()).doesNotContainKeys("email", "password", "passwordHash", "legalName", "taxId");
    }
}
