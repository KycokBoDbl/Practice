package ru.esie.practice.roomhubb2b.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TokenPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class
            ))
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "roomhub.auth.token.issuer=roomhub-test",
                    "roomhub.auth.token.ttl=PT15M"
            );

    @Test
    void bindsValidTokenConfiguration() {
        contextRunner
                .withPropertyValues("roomhub.auth.token.secret=12345678901234567890123456789012")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    TokenProperties properties = context.getBean(TokenProperties.class);
                    assertThat(properties.issuer()).isEqualTo("roomhub-test");
                    assertThat(properties.ttl()).isEqualTo(Duration.ofMinutes(15));
                });
    }

    @Test
    void rejectsSigningSecretShorterThan256Bits() {
        contextRunner
                .withPropertyValues("roomhub.auth.token.secret=too-short")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNonPositiveTtl() {
        contextRunner
                .withPropertyValues(
                        "roomhub.auth.token.secret=12345678901234567890123456789012",
                        "roomhub.auth.token.ttl=PT0S"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TokenProperties.class)
    static class TestConfiguration {

        @Bean
        static LocalValidatorFactoryBean configurationPropertiesValidator() {
            return new LocalValidatorFactoryBean();
        }
    }
}
