package ru.esie.practice.roomhubb2b.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "roomhub.geocoding.yandex")
public class YandexGeocodingProperties {

    @NotBlank
    private String apiKey;

    @NotNull
    private URI baseUrl = URI.create("https://geocode-maps.yandex.ru/v1");

    @NotBlank
    @Pattern(regexp = "^[a-z]{2}_[A-Z]{2}$")
    private String lang = "ru_RU";

    @Min(1)
    @Max(10)
    private int results = 1;

    @NotNull
    private Duration timeout = Duration.ofSeconds(3);

    public String apiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public URI baseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String lang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public int results() {
        return results;
    }

    public void setResults(int results) {
        this.results = results;
    }

    public Duration timeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    @AssertTrue(message = "roomhub.geocoding.yandex.timeout must be positive")
    public boolean isTimeoutPositive() {
        return timeout != null && !timeout.isNegative() && !timeout.isZero();
    }

    @AssertTrue(message = "roomhub.geocoding.yandex.base-url must be an absolute HTTP(S) URI")
    public boolean isBaseUrlAbsoluteHttpUri() {
        if (baseUrl == null) {
            return false;
        }
        String scheme = baseUrl.getScheme();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && baseUrl.getHost() != null;
    }
}
