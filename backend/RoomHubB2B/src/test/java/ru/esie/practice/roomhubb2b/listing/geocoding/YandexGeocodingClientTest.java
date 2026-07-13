package ru.esie.practice.roomhubb2b.listing.geocoding;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.esie.practice.roomhubb2b.config.YandexGeocodingProperties;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YandexGeocodingClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesCoordinatesFromYandexResponse() throws IOException {
        AtomicReference<String> query = new AtomicReference<>();
        startServer(200, yandexResponse("83.779836 53.348114"), Duration.ZERO, query);

        GeoCoordinates coordinates = client(Duration.ofSeconds(1)).geocode("Barnaul", "Lenina Avenue, 10");

        assertThat(coordinates.latitude()).isEqualByComparingTo("53.348114");
        assertThat(coordinates.longitude()).isEqualByComparingTo("83.779836");
        assertThat(query.get()).contains("apikey=test-key", "format=json", "results=1", "lang=ru_RU");
    }

    @Test
    void throwsBadRequestExceptionForEmptyResult() throws IOException {
        startServer(200, """
                {
                  "response": {
                    "GeoObjectCollection": {
                      "featureMember": []
                    }
                  }
                }
                """, Duration.ZERO, new AtomicReference<>());

        assertThatThrownBy(() -> client(Duration.ofSeconds(1)).geocode("Unknown", "No street"))
                .isInstanceOf(AddressNotGeocodedException.class)
                .hasMessage("Listing address could not be geocoded");
    }

    @Test
    void rejectsInvalidCoordinateRange() throws IOException {
        startServer(200, yandexResponse("200.000000 95.000000"), Duration.ZERO, new AtomicReference<>());

        assertThatThrownBy(() -> client(Duration.ofSeconds(1)).geocode("Barnaul", "Lenina Avenue, 10"))
                .isInstanceOf(GeocodingProviderException.class)
                .hasMessage("Yandex geocoder response has invalid coordinates");
    }

    @Test
    void mapsHttpErrorToProviderException() throws IOException {
        startServer(403, "{\"message\":\"Invalid apikey\"}", Duration.ZERO, new AtomicReference<>());

        assertThatThrownBy(() -> client(Duration.ofSeconds(1)).geocode("Barnaul", "Lenina Avenue, 10"))
                .isInstanceOf(GeocodingProviderException.class)
                .hasMessage("Yandex geocoder request failed");
    }

    @Test
    void mapsTimeoutToProviderException() throws IOException {
        startServer(200, yandexResponse("83.779836 53.348114"), Duration.ofMillis(500), new AtomicReference<>());

        assertThatThrownBy(() -> client(Duration.ofMillis(50)).geocode("Barnaul", "Lenina Avenue, 10"))
                .isInstanceOf(GeocodingProviderException.class)
                .hasMessage("Yandex geocoder is unavailable");
    }

    private YandexGeocodingClient client(Duration timeout) {
        YandexGeocodingProperties properties = new YandexGeocodingProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl(URI.create("http://localhost:" + server.getAddress().getPort()));
        properties.setLang("ru_RU");
        properties.setResults(1);
        properties.setTimeout(timeout);
        return new YandexGeocodingClient(properties, new ObjectMapper());
    }

    private void startServer(
            int status,
            String response,
            Duration delay,
            AtomicReference<String> query
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            if (!delay.isZero()) {
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    private String yandexResponse(String pos) {
        return """
                {
                  "response": {
                    "GeoObjectCollection": {
                      "featureMember": [
                        {
                          "GeoObject": {
                            "Point": {
                              "pos": "%s"
                            }
                          }
                        }
                      ]
                    }
                  }
                }
                """.formatted(pos);
    }
}
