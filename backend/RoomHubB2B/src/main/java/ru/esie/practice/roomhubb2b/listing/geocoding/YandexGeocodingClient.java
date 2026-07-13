package ru.esie.practice.roomhubb2b.listing.geocoding;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.esie.practice.roomhubb2b.config.YandexGeocodingProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class YandexGeocodingClient implements GeocodingService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final YandexGeocodingProperties properties;

    public YandexGeocodingClient(YandexGeocodingProperties properties, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public GeoCoordinates geocode(String city, String address) {
        String query = Stream.of(city, address)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(", "));
        if (!StringUtils.hasText(query)) {
            throw new AddressNotGeocodedException("Listing address could not be geocoded");
        }

        String response = request(query);
        return parseCoordinates(response);
    }

    GeoCoordinates parseCoordinates(String response) {
        try {
            JsonNode featureMember = objectMapper.readTree(response)
                    .path("response")
                    .path("GeoObjectCollection")
                    .path("featureMember");
            if (!featureMember.isArray() || featureMember.isEmpty()) {
                throw new AddressNotGeocodedException("Listing address could not be geocoded");
            }

            JsonNode pos = featureMember.get(0)
                    .path("GeoObject")
                    .path("Point")
                    .path("pos");
            if (!pos.isTextual()) {
                throw new GeocodingProviderException("Yandex geocoder response does not contain coordinates");
            }
            String[] parts = pos.asText().trim().split("\\s+");
            if (parts.length != 2) {
                throw new GeocodingProviderException("Yandex geocoder response has invalid coordinate format");
            }

            BigDecimal longitude = new BigDecimal(parts[0]);
            BigDecimal latitude = new BigDecimal(parts[1]);
            return new GeoCoordinates(latitude, longitude);
        } catch (AddressNotGeocodedException | GeocodingProviderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new GeocodingProviderException("Yandex geocoder response has invalid coordinates", exception);
        }
    }

    private String request(String query) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("apikey", properties.apiKey())
                            .queryParam("geocode", query)
                            .queryParam("lang", properties.lang())
                            .queryParam("format", "json")
                            .queryParam("results", properties.results())
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw new GeocodingProviderException("Yandex geocoder request failed", exception);
        } catch (RestClientException exception) {
            throw new GeocodingProviderException("Yandex geocoder is unavailable", exception);
        }
    }
}
