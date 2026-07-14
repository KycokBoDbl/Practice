package ru.esie.practice.roomhubb2b.listing.ai;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class GigaChatClient implements GigaChatCompletionClient {

    private static final String SYSTEM_PROMPT = """
            Extract a RoomHub listing search filter from the user prompt.
            Return only valid JSON with these keys: city, spaceType, minCapacity, maxPricePerHour, limit.
            Use null for unknown values.
            spaceType must be one of MEETING_ROOM, CONFERENCE_HALL, CLASSROOM, LOFT, SHOWROOM, or null.
            minCapacity and limit must be positive integers when present.
            maxPricePerHour must be a positive decimal when present.
            Do not include explanations, markdown, SQL, or extra keys.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GigaChatProperties properties;
    private final GigaChatTokenClient tokenClient;

    public GigaChatClient(GigaChatProperties properties, ObjectMapper objectMapper, GigaChatTokenClient tokenClient) {
        this.restClient = GigaChatRestClientFactory.create(properties);
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.tokenClient = tokenClient;
    }

    @Override
    public String extractListingFilterJson(String prompt) {
        try {
            String response = restClient.post()
                    .uri(properties.chatUrl())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(tokenClient.accessToken()))
                    .body(requestBody(prompt))
                    .retrieve()
                    .body(String.class);
            return parseContent(response);
        } catch (RestClientResponseException exception) {
            throw new AiListingSearchException("GigaChat model request failed", exception);
        } catch (RestClientException exception) {
            throw new AiListingSearchException("GigaChat model is unavailable", exception);
        }
    }

    private Map<String, Object> requestBody(String prompt) {
        return Map.of(
                "model", properties.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0,
                "stream", false
        );
    }

    String parseContent(String response) {
        try {
            JsonNode content = objectMapper.readTree(response)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new AiListingSearchException("GigaChat model response does not contain filter content");
            }
            return content.asText();
        } catch (AiListingSearchException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiListingSearchException("GigaChat model response is invalid", exception);
        }
    }
}
