package ru.esie.practice.roomhubb2b.listing.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GigaChatClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsPromptWithBearerTokenAndParsesContent() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer(200, completionResponse("{\\\"city\\\":\\\"Barnaul\\\",\\\"limit\\\":5}"), Duration.ZERO,
                authorization, requestBody);

        String content = client(Duration.ofSeconds(1)).extractListingFilterJson("Need a room in Barnaul");

        assertThat(content).isEqualTo("{\"city\":\"Barnaul\",\"limit\":5}");
        assertThat(authorization.get()).isEqualTo("Bearer test-access-token");
        assertThat(requestBody.get()).contains("GigaChat", "Need a room in Barnaul", "spaceType");
    }

    @Test
    void mapsModelHttpError() throws IOException {
        startServer(500, "{\"message\":\"failure\"}", Duration.ZERO,
                new AtomicReference<>(), new AtomicReference<>());

        assertThatThrownBy(() -> client(Duration.ofSeconds(1)).extractListingFilterJson("prompt"))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat model request failed");
    }

    @Test
    void mapsModelTimeout() throws IOException {
        startServer(200, completionResponse("{}"), Duration.ofMillis(500),
                new AtomicReference<>(), new AtomicReference<>());

        assertThatThrownBy(() -> client(Duration.ofMillis(50)).extractListingFilterJson("prompt"))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat model is unavailable");
    }

    @Test
    void rejectsInvalidModelPayload() {
        GigaChatClient client = clientWithoutServer();

        assertThatThrownBy(() -> client.parseContent("{\"choices\":[]}"))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat model response does not contain filter content");
    }

    private GigaChatClient client(Duration timeout) {
        GigaChatProperties properties = properties(timeout);
        properties.setChatUrl(URI.create("http://localhost:" + server.getAddress().getPort() + "/chat"));
        GigaChatTokenClient tokenClient = mock(GigaChatTokenClient.class);
        when(tokenClient.accessToken()).thenReturn("test-access-token");
        return new GigaChatClient(properties, new ObjectMapper(), tokenClient);
    }

    private GigaChatClient clientWithoutServer() {
        GigaChatTokenClient tokenClient = mock(GigaChatTokenClient.class);
        return new GigaChatClient(properties(Duration.ofSeconds(1)), new ObjectMapper(), tokenClient);
    }

    private GigaChatProperties properties(Duration timeout) {
        GigaChatProperties properties = new GigaChatProperties();
        properties.setAuthorizationKey("test-authorization-key");
        properties.setTimeout(timeout);
        properties.setModel("GigaChat");
        return properties;
    }

    private void startServer(
            int status,
            String response,
            Duration delay,
            AtomicReference<String> authorization,
            AtomicReference<String> requestBody
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (!delay.isZero()) {
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] responseBody = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
    }

    private String completionResponse(String content) {
        return """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "%s"
                      }
                    }
                  ]
                }
                """.formatted(content);
    }
}
