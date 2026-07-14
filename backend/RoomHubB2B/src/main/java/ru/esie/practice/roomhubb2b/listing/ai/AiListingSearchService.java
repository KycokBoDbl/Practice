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
import java.util.Locale;
import java.util.List;
import java.util.Set;

@Service
public class AiListingSearchService {

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            "city",
            "spaceType",
            "minCapacity",
            "maxPricePerHour",
            "limit"
    );

    private final ListingRepository listingRepository;
    private final GigaChatCompletionClient gigaChatClient;
    private final GigaChatProperties properties;
    private final ObjectMapper objectMapper;

    public AiListingSearchService(
            ListingRepository listingRepository,
            GigaChatCompletionClient gigaChatClient,
            GigaChatProperties properties,
            ObjectMapper objectMapper
    ) {
        this.listingRepository = listingRepository;
        this.gigaChatClient = gigaChatClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<ListingResponseDto> search(AiListingSearchRequestDto request) {
        String prompt = normalizePrompt(request.prompt());
        if (prompt.length() > properties.maxPromptLength()) {
            throw new IllegalArgumentException("prompt length must be less than or equal to "
                    + properties.maxPromptLength());
        }

        AiListingSearchFilter filter = parseFilter(gigaChatClient.extractListingFilterJson(prompt));
        return listingRepository.searchPublished(
                        ListingStatus.PUBLISHED,
                        filter.city(),
                        filter.spaceType(),
                        filter.minCapacity(),
                        filter.maxPricePerHour(),
                        PageRequest.of(0, filter.limit())
                )
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    AiListingSearchFilter parseFilter(String content) {
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
            String city = normalizedCityOrNull(root.path("city"));
            SpaceType spaceType = spaceTypeOrNull(root.path("spaceType"));
            Integer minCapacity = positiveIntegerOrNull(root.path("minCapacity"));
            BigDecimal maxPricePerHour = positiveDecimalOrNull(root.path("maxPricePerHour"));
            int limit = limitOrDefault(root.path("limit"));
            return new AiListingSearchFilter(city, spaceType, minCapacity, maxPricePerHour, limit);
        } catch (AiListingSearchException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidFilter();
        }
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

    private String normalizedCityOrNull(JsonNode node) {
        String city = textOrNull(node);
        return city == null ? null : city.toLowerCase(Locale.ROOT);
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
}
