package ru.esie.practice.roomhubb2b.auth;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import ru.esie.practice.roomhubb2b.auth.dto.TokenResponseDto;
import ru.esie.practice.roomhubb2b.config.TokenProperties;

import java.time.Clock;
import java.time.Instant;

@Service
public class AccessTokenService {

    private final JwtEncoder jwtEncoder;
    private final TokenProperties properties;
    private final Clock clock;

    public AccessTokenService(JwtEncoder jwtEncoder, TokenProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenResponseDto issue(UserEntity user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("organizationId", user.getOrganization().getId())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponseDto(token, "Bearer", properties.ttl().toSeconds());
    }
}
