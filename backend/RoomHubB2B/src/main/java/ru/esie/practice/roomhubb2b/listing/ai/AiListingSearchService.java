package ru.esie.practice.roomhubb2b.listing.ai;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.listing.ListingEntity;
import ru.esie.practice.roomhubb2b.listing.ListingRepository;
import ru.esie.practice.roomhubb2b.listing.ListingStatus;
import ru.esie.practice.roomhubb2b.listing.SpaceType;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AiListingSearchService {

    private static final LocalDateTime UNUSED_AVAILABLE_FROM = LocalDateTime.of(1, 1, 1, 0, 0);
    private static final LocalDateTime UNUSED_AVAILABLE_TO = LocalDateTime.of(9999, 12, 31, 0, 0);

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            "city",
            "spaceType",
            "minCapacity",
            "minPricePerHour",
            "maxPricePerHour",
            "availableFrom",
            "availableTo",
            "ignoredTerms",
            "limit"
    );

    private final ListingRepository listingRepository;
    private final GigaChatCompletionClient gigaChatClient;
    private final GigaChatProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AiListingSearchService(
            ListingRepository listingRepository,
            GigaChatCompletionClient gigaChatClient,
            GigaChatProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.listingRepository = listingRepository;
        this.gigaChatClient = gigaChatClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public AiListingSearchResponseDto search(AiListingSearchRequestDto request) {
        String prompt = normalizePrompt(request.prompt());
        if (prompt.length() > properties.maxPromptLength()) {
            throw new IllegalArgumentException("prompt length must be less than or equal to "
                    + properties.maxPromptLength());
        }

        List<String> allowedCities = listingRepository.findDistinctCitiesByStatus(ListingStatus.PUBLISHED);
        AiListingSearchInterpretation interpretation = parseInterpretation(
                gigaChatClient.extractListingFilterJson(promptWithContext(prompt, allowedCities))
        );
        AiListingSearchFilter filter = validateFilter(interpretation.filter(), allowedCities);
        boolean availabilityRequired = filter.availableFrom() != null;
        List<ListingResponseDto> results = listingRepository.searchPublished(
                        ListingStatus.PUBLISHED,
                        filter.city() == null ? null : filter.city().toLowerCase(Locale.ROOT),
                        filter.spaceType(),
                        filter.minCapacity(),
                        filter.minPricePerHour(),
                        filter.maxPricePerHour(),
                        availabilityRequired,
                        availabilityRequired ? filter.availableFrom() : UNUSED_AVAILABLE_FROM,
                        availabilityRequired ? filter.availableTo() : UNUSED_AVAILABLE_TO,
                        PageRequest.of(0, filter.limit())
                )
                .stream()
                .map(this::toResponseDto)
                .toList();

        return new AiListingSearchResponseDto(
                toInterpretedFilterDto(filter),
                interpretation.ignoredTerms(),
                results
        );
    }

    AiListingSearchFilter parseFilter(String content) {
        return parseInterpretation(content).filter();
    }

    private AiListingSearchInterpretation parseInterpretation(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            if (!root.isObject()) {
                throw invalidFilter();
            }
            for (String fieldName : root.propertyNames()) {
                if (!SUPPORTED_FIELDS.contains(fieldName)) {
                    throw invalidFilter();
                }
            }
            String city = textOrNull(root.path("city"));
            SpaceType spaceType = spaceTypeOrNull(root.path("spaceType"));
            Integer minCapacity = positiveIntegerOrNull(root.path("minCapacity"));
            BigDecimal minPricePerHour = positiveDecimalOrNull(root.path("minPricePerHour"));
            BigDecimal maxPricePerHour = positiveDecimalOrNull(root.path("maxPricePerHour"));
            LocalDateTime availableFrom = localDateTimeOrNull(root.path("availableFrom"), false);
            LocalDateTime availableTo = localDateTimeOrNull(root.path("availableTo"), true);
            int limit = limitOrDefault(root.path("limit"));
            validatePriceRange(minPricePerHour, maxPricePerHour);
            validateAvailabilityRange(availableFrom, availableTo);
            return new AiListingSearchInterpretation(
                    new AiListingSearchFilter(
                            city,
                            spaceType,
                            minCapacity,
                            minPricePerHour,
                            maxPricePerHour,
                            availableFrom,
                            availableTo,
                            limit
                    ),
                    ignoredTermsOrEmpty(root.path("ignoredTerms"))
            );
        } catch (AiListingSearchException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidFilter();
        }
    }

    private String promptWithContext(String prompt, List<String> allowedCities) {
        List<String> allowedSpaceTypes = Arrays.stream(SpaceType.values())
                .map(Enum::name)
                .toList();
        return "Current date: " + LocalDate.now(clock)
                + ". Interpret relative availability phrases against this date.\n"
                + "Allowed cities: " + jsonArray(allowedCities) + "\n"
                + "City rule: if the user mentions a city, map it to exactly one value from Allowed cities. "
                + "Return null when none matches confidently. Do not invent cities.\n"
                + "Allowed space types: " + jsonArray(allowedSpaceTypes) + "\n"
                + "Space type rule: if the user describes the type of space, map it to exactly one value "
                + "from Allowed space types. Return null when none matches confidently.\n"
                + "User prompt: " + prompt;
    }

    private AiListingSearchFilter validateFilter(AiListingSearchFilter filter, List<String> allowedCities) {
        return new AiListingSearchFilter(
                validateCity(filter.city(), allowedCities),
                filter.spaceType(),
                filter.minCapacity(),
                filter.minPricePerHour(),
                filter.maxPricePerHour(),
                filter.availableFrom(),
                filter.availableTo(),
                filter.limit()
        );
    }

    private String jsonArray(List<String> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(jsonString(values.get(index)));
        }
        return builder.append(']').toString();
    }

    private String jsonString(String value) {
        StringBuilder builder = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"' || character == '\\') {
                builder.append('\\');
            }
            builder.append(character);
        }
        return builder.append('"').toString();
    }

    private String normalizePrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        return prompt.trim();
    }

    private String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw invalidFilter();
        }
        String value = node.asText().trim();
        return value.isBlank() ? null : value;
    }

    private SpaceType spaceTypeOrNull(JsonNode node) {
        String value = textOrNull(node);
        if (value == null) {
            return null;
        }
        try {
            return SpaceType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalidFilter();
        }
    }

    private Integer positiveIntegerOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isIntegralNumber() || node.asInt() <= 0) {
            throw invalidFilter();
        }
        return node.asInt();
    }

    private BigDecimal positiveDecimalOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isNumber()) {
            throw invalidFilter();
        }
        BigDecimal value = node.decimalValue();
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidFilter();
        }
        return value;
    }

    private LocalDateTime localDateTimeOrNull(JsonNode node, boolean endBoundary) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw invalidFilter();
        }
        String value = node.asText().trim();
        if (value.isBlank()) {
            return null;
        }
        String normalized = value.toUpperCase();
        LocalDate today = LocalDate.now(clock);
        return switch (normalized) {
            case "TODAY", "TODAY_START" -> today.atStartOfDay();
            case "TODAY_END" -> today.plusDays(1).atStartOfDay();
            case "TOMORROW", "TOMORROW_START" -> today.plusDays(1).atStartOfDay();
            case "TOMORROW_END" -> today.plusDays(2).atStartOfDay();
            default -> parseDateTime(value, endBoundary);
        };
    }

    private LocalDateTime parseDateTime(String value, boolean endBoundary) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate date = LocalDate.parse(value);
                return endBoundary ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
            } catch (DateTimeParseException exception) {
                throw invalidFilter();
            }
        }
    }

    private List<String> ignoredTermsOrEmpty(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw invalidFilter();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw invalidFilter();
            }
            String value = item.asText().trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private void validatePriceRange(BigDecimal minPricePerHour, BigDecimal maxPricePerHour) {
        if (minPricePerHour != null
                && maxPricePerHour != null
                && minPricePerHour.compareTo(maxPricePerHour) > 0) {
            throw invalidFilter();
        }
    }

    private void validateAvailabilityRange(LocalDateTime availableFrom, LocalDateTime availableTo) {
        if ((availableFrom == null) != (availableTo == null)) {
            throw invalidFilter();
        }
        if (availableFrom != null && !availableFrom.isBefore(availableTo)) {
            throw invalidFilter();
        }
        if ((availableFrom != null && !isWholeHour(availableFrom))
                || (availableTo != null && !isWholeHour(availableTo))) {
            throw invalidFilter();
        }
    }

    private boolean isWholeHour(LocalDateTime value) {
        return value.getMinute() == 0 && value.getSecond() == 0 && value.getNano() == 0;
    }

    private int limitOrDefault(JsonNode node) {
        Integer limit = positiveIntegerOrNull(node);
        if (limit == null) {
            return properties.defaultLimit();
        }
        if (limit > properties.maxLimit()) {
            return properties.maxLimit();
        }
        return limit;
    }

    private AiListingSearchException invalidFilter() {
        return new AiListingSearchException("GigaChat model returned invalid listing filter");
    }

    private String validateCity(String city, List<String> allowedCities) {
        if (city == null) {
            return null;
        }
        if (!allowedCities.contains(city)) {
            throw invalidFilter();
        }
        return city;
    }

    private AiListingSearchInterpretedFilterDto toInterpretedFilterDto(AiListingSearchFilter filter) {
        return new AiListingSearchInterpretedFilterDto(
                filter.city(),
                filter.spaceType(),
                filter.minCapacity(),
                filter.minPricePerHour(),
                filter.maxPricePerHour(),
                filter.availableFrom(),
                filter.availableTo(),
                filter.limit()
        );
    }

    private ListingResponseDto toResponseDto(ListingEntity listing) {
        OrganizationEntity owner = listing.getOwnerOrganization();
        return new ListingResponseDto(
                listing.getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getPricePerHour(),
                listing.getCapacity(),
                listing.getSpaceType(),
                listing.getImageUrl(),
                listing.getDescription(),
                listing.getAddress(),
                owner == null ? null : owner.getLegalName(),
                listing.getLatitude(),
                listing.getLongitude()
        );
    }

    private record AiListingSearchInterpretation(
            AiListingSearchFilter filter,
            List<String> ignoredTerms
    ) {
    }
}
