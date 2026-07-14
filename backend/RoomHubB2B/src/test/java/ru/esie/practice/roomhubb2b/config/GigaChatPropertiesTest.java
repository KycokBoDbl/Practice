package ru.esie.practice.roomhubb2b.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.esie.practice.roomhubb2b.listing.ai.GigaChatProperties;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GigaChatPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsValidGigaChatConfiguration() {
        contextRunner
                .withPropertyValues("roomhub.ai.gigachat.authorization-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    GigaChatProperties properties = context.getBean(GigaChatProperties.class);
                    assertThat(properties.authorizationKey()).isEqualTo("test-key");
                    assertThat(properties.oauthUrl()).isEqualTo(URI.create("https://ngw.devices.sberbank.ru:9443/api/v2/oauth"));
                    assertThat(properties.chatUrl()).isEqualTo(URI.create("https://gigachat.devices.sberbank.ru/api/v1/chat/completions"));
                    assertThat(properties.model()).isEqualTo("GigaChat");
                    assertThat(properties.scope()).isEqualTo("GIGACHAT_API_PERS");
                    assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(5));
                    assertThat(properties.maxPromptLength()).isEqualTo(1000);
                    assertThat(properties.defaultLimit()).isEqualTo(10);
                    assertThat(properties.maxLimit()).isEqualTo(20);
                });
    }

    @Test
    void rejectsBlankAuthorizationKey() {
        contextRunner
                .withPropertyValues("roomhub.ai.gigachat.authorization-key=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsRelativeUrls() {
        contextRunner
                .withPropertyValues(
                        "roomhub.ai.gigachat.authorization-key=test-key",
                        "roomhub.ai.gigachat.oauth-url=oauth",
                        "roomhub.ai.gigachat.chat-url=chat"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsInvalidLimits() {
        contextRunner
                .withPropertyValues(
                        "roomhub.ai.gigachat.authorization-key=test-key",
                        "roomhub.ai.gigachat.default-limit=30",
                        "roomhub.ai.gigachat.max-limit=20"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNonPositiveTimeout() {
        contextRunner
                .withPropertyValues(
                        "roomhub.ai.gigachat.authorization-key=test-key",
                        "roomhub.ai.gigachat.timeout=PT0S"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GigaChatProperties.class)
    static class TestConfiguration {

        @Bean
        static LocalValidatorFactoryBean configurationPropertiesValidator() {
            return new LocalValidatorFactoryBean();
        }
    }
}
