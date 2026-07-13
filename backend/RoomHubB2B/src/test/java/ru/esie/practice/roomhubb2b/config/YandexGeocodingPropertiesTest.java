package ru.esie.practice.roomhubb2b.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class YandexGeocodingPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class
            ))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsValidYandexGeocodingConfiguration() {
        contextRunner
                .withPropertyValues("roomhub.geocoding.yandex.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    YandexGeocodingProperties properties = context.getBean(YandexGeocodingProperties.class);
                    assertThat(properties.apiKey()).isEqualTo("test-key");
                    assertThat(properties.baseUrl()).isEqualTo(URI.create("https://geocode-maps.yandex.ru/v1"));
                    assertThat(properties.lang()).isEqualTo("ru_RU");
                    assertThat(properties.results()).isEqualTo(1);
                    assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(3));
                });
    }

    @Test
    void rejectsBlankApiKey() {
        contextRunner
                .withPropertyValues("roomhub.geocoding.yandex.api-key=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsRelativeBaseUrl() {
        contextRunner
                .withPropertyValues(
                        "roomhub.geocoding.yandex.api-key=test-key",
                        "roomhub.geocoding.yandex.base-url=geocode-maps.yandex.ru/v1"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsInvalidLangFormat() {
        contextRunner
                .withPropertyValues(
                        "roomhub.geocoding.yandex.api-key=test-key",
                        "roomhub.geocoding.yandex.lang=ru-ru"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsTooManyResults() {
        contextRunner
                .withPropertyValues(
                        "roomhub.geocoding.yandex.api-key=test-key",
                        "roomhub.geocoding.yandex.results=11"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNonPositiveTimeout() {
        contextRunner
                .withPropertyValues(
                        "roomhub.geocoding.yandex.api-key=test-key",
                        "roomhub.geocoding.yandex.timeout=PT0S"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(YandexGeocodingProperties.class)
    static class TestConfiguration {

        @Bean
        static LocalValidatorFactoryBean configurationPropertiesValidator() {
            return new LocalValidatorFactoryBean();
        }
    }
}
