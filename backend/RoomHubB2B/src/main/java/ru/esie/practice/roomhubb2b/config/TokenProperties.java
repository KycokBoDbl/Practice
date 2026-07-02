package ru.esie.practice.roomhubb2b.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "roomhub.auth.token")
public record TokenProperties(
        @NotBlank String issuer,
        @NotNull Duration ttl,
        @NotBlank String secret
) {
    private static final int MINIMUM_SECRET_BYTES = 32;

    @AssertTrue(message = "roomhub.auth.token.secret must contain at least 32 UTF-8 bytes")
    public boolean isSecretStrongEnough() {
        return secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= MINIMUM_SECRET_BYTES;
    }

    @AssertTrue(message = "roomhub.auth.token.ttl must be positive")
    public boolean isTtlPositive() {
        return ttl != null && !ttl.isZero() && !ttl.isNegative();
    }
}
