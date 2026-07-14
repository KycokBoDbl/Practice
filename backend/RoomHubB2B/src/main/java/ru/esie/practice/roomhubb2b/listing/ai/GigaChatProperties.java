package ru.esie.practice.roomhubb2b.listing.ai;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "roomhub.ai.gigachat")
public class GigaChatProperties {

    @NotBlank
    private String authorizationKey;

    @NotNull
    private URI oauthUrl = URI.create("https://ngw.devices.sberbank.ru:9443/api/v2/oauth");

    @NotNull
    private URI chatUrl = URI.create("https://gigachat.devices.sberbank.ru/api/v1/chat/completions");

    @NotBlank
    private String model = "GigaChat";

    @NotBlank
    private String scope = "GIGACHAT_API_PERS";

    @NotNull
    private Duration timeout = Duration.ofSeconds(5);

    @NotNull
    private Duration tokenSafetySkew = Duration.ofSeconds(30);

    private boolean insecureSkipTlsVerification = false;

    @Min(1)
    @Max(AiListingSearchRequestDto.MAX_PROMPT_LENGTH)
    private int maxPromptLength = AiListingSearchRequestDto.MAX_PROMPT_LENGTH;

    @Min(1)
    @Max(100)
    private int defaultLimit = 10;

    @Min(1)
    @Max(100)
    private int maxLimit = 20;

    public String authorizationKey() {
        return authorizationKey;
    }

    public void setAuthorizationKey(String authorizationKey) {
        this.authorizationKey = authorizationKey;
    }

    public URI oauthUrl() {
        return oauthUrl;
    }

    public void setOauthUrl(URI oauthUrl) {
        this.oauthUrl = oauthUrl;
    }

    public URI chatUrl() {
        return chatUrl;
    }

    public void setChatUrl(URI chatUrl) {
        this.chatUrl = chatUrl;
    }

    public String model() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String scope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Duration timeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration tokenSafetySkew() {
        return tokenSafetySkew;
    }

    public void setTokenSafetySkew(Duration tokenSafetySkew) {
        this.tokenSafetySkew = tokenSafetySkew;
    }

    public boolean insecureSkipTlsVerification() {
        return insecureSkipTlsVerification;
    }

    public void setInsecureSkipTlsVerification(boolean insecureSkipTlsVerification) {
        this.insecureSkipTlsVerification = insecureSkipTlsVerification;
    }

    public int maxPromptLength() {
        return maxPromptLength;
    }

    public void setMaxPromptLength(int maxPromptLength) {
        this.maxPromptLength = maxPromptLength;
    }

    public int defaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int maxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    @AssertTrue(message = "roomhub.ai.gigachat.oauth-url must be an absolute HTTP(S) URI")
    public boolean isOauthUrlAbsoluteHttpUri() {
        return isAbsoluteHttpUri(oauthUrl);
    }

    @AssertTrue(message = "roomhub.ai.gigachat.chat-url must be an absolute HTTP(S) URI")
    public boolean isChatUrlAbsoluteHttpUri() {
        return isAbsoluteHttpUri(chatUrl);
    }

    @AssertTrue(message = "roomhub.ai.gigachat.timeout must be positive")
    public boolean isTimeoutPositive() {
        return timeout != null && !timeout.isNegative() && !timeout.isZero();
    }

    @AssertTrue(message = "roomhub.ai.gigachat.token-safety-skew must not be negative")
    public boolean isTokenSafetySkewNotNegative() {
        return tokenSafetySkew != null && !tokenSafetySkew.isNegative();
    }

    @AssertTrue(message = "roomhub.ai.gigachat.default-limit must not exceed max-limit")
    public boolean isDefaultLimitWithinMaxLimit() {
        return defaultLimit <= maxLimit;
    }

    private static boolean isAbsoluteHttpUri(URI uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && uri.getHost() != null;
    }
}
