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
            Извлеки фильтр поиска объявлений RoomHub из живого русского или английского запроса.
            Return only valid JSON with these keys:
            city, spaceType, minCapacity, minPricePerHour, maxPricePerHour, availableFrom, availableTo, ignoredTerms, limit.
            Use null for unknown scalar values and [] for ignoredTerms when every meaningful term is represented.
            minCapacity and limit must be positive integers when present.
            minPricePerHour and maxPricePerHour must be positive decimals when present.
            availableFrom and availableTo must be ISO-8601 local date-time strings aligned to whole hours, or null.
            Interpret relative dates against the "Current date" line in the user message:
            - "сегодня" means current date 00:00 to next date 00:00.
            - "завтра" means current date plus 1 day 00:00 to current date plus 2 days 00:00.
            The user message contains an Allowed cities JSON array.
            If the user mentions a city, normalize Russian cases, abbreviations, transliteration, and common synonyms
            to exactly one string from Allowed cities.
            city must be either exactly one value from Allowed cities or null.
            Return null when no allowed city matches confidently. Never invent a city and never return a city outside Allowed cities.
            The user message contains an Allowed space types JSON array.
            If the user describes a type of room or venue, normalize Russian wording, synonyms, and intent
            to exactly one string from Allowed space types.
            spaceType must be either exactly one value from Allowed space types or null.
            Return null when no allowed space type matches confidently. Never invent a spaceType and never return a value outside Allowed space types.
            Type hints:
            - переговорка, комната для созвона, встреча -> MEETING_ROOM.
            - конференция, зал, лекция, корпоратив -> CONFERENCE_HALL.
            - класс, тренинг, обучение, проектор для занятия -> CLASSROOM.
            - лофт, фотосессия, вечеринка, неформальная встреча -> LOFT.
            - шоурум, pop-up, показ, презентация продукта -> SHOWROOM.
            Treat supported fields as hard filters. Put meaningful unsupported preferences into ignoredTerms instead of forcing them into hard filters:
            район, метро, стиль, оборудование, этаж, парковка, удобства, "уютный", "с экраном", "с проектором".
            Examples:
            User: Current date: 2026-07-15. Allowed cities: ["Москва","Санкт-Петербург"]. Allowed space types: ["MEETING_ROOM","CONFERENCE_HALL","CLASSROOM","LOFT","SHOWROOM"]. User prompt: Нужен уютный лофт в Москве на завтра до 5000 в час
            JSON: {"city":"Москва","spaceType":"LOFT","minCapacity":null,"minPricePerHour":null,"maxPricePerHour":5000,"availableFrom":"2026-07-16T00:00","availableTo":"2026-07-17T00:00","ignoredTerms":["уютный"],"limit":null}
            User: Current date: 2026-07-15. Allowed cities: ["Москва","Санкт-Петербург"]. Allowed space types: ["MEETING_ROOM","CONFERENCE_HALL","CLASSROOM","LOFT","SHOWROOM"]. User prompt: переговорка в мск от 1000 до 3000 для 8 человек с экраном
            JSON: {"city":"Москва","spaceType":"MEETING_ROOM","minCapacity":8,"minPricePerHour":1000,"maxPricePerHour":3000,"availableFrom":null,"availableTo":null,"ignoredTerms":["с экраном"],"limit":null}
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
