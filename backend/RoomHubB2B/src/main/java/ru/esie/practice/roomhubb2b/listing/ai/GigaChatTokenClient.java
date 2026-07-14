package ru.esie.practice.roomhubb2b.listing.ai;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class GigaChatTokenClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GigaChatProperties properties;
    private final Clock clock;
    private AccessToken cachedToken;

    public GigaChatTokenClient(GigaChatProperties properties, ObjectMapper objectMapper, Clock clock) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized String accessToken() {
        if (cachedToken != null && cachedToken.isUsable(clock.instant(), properties.tokenSafetySkew())) {
            return cachedToken.value();
        }
        cachedToken = requestToken();
        return cachedToken.value();
    }

    private AccessToken requestToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("scope", properties.scope());
        try {
            String response = restClient.post()
                    .uri(properties.oauthUrl())
                    .header("RqUID", UUID.randomUUID().toString())
                    .header("Authorization", "Basic " + properties.authorizationKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseToken(response);
        } catch (RestClientResponseException exception) {
            throw new AiListingSearchException("GigaChat OAuth request failed", exception);
        } catch (RestClientException exception) {
            throw new AiListingSearchException("GigaChat OAuth is unavailable", exception);
        }
    }

    AccessToken parseToken(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode token = root.path("access_token");
            if (!token.isTextual() || token.asText().isBlank()) {
                throw new AiListingSearchException("GigaChat OAuth response does not contain access token");
            }
            Instant expiresAt = parseExpiration(root.path("expires_at"));
            return new AccessToken(token.asText(), expiresAt);
        } catch (AiListingSearchException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiListingSearchException("GigaChat OAuth response is invalid", exception);
        }
    }

    private Instant parseExpiration(JsonNode expiresAt) {
        if (!expiresAt.isNumber()) {
            throw new AiListingSearchException("GigaChat OAuth response does not contain token expiration");
        }
        long raw = expiresAt.asLong();
        if (raw > 1_000_000_000_000L) {
            return Instant.ofEpochMilli(raw);
        }
        return Instant.ofEpochSecond(raw);
    }
}
