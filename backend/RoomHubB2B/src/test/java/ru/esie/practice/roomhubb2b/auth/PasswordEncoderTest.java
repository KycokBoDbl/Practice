package ru.esie.practice.roomhubb2b.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    @Test
    void createsSaltedAdaptiveHashes() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String password = "S3cure-roomhub-password";

        String first = encoder.encode(password);
        String second = encoder.encode(password);

        assertThat(first).startsWith("{bcrypt}").isNotEqualTo(password).isNotEqualTo(second);
        assertThat(encoder.matches(password, first)).isTrue();
        assertThat(encoder.matches(password, second)).isTrue();
    }
}
