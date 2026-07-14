package ru.esie.practice.roomhubb2b.listing.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GigaChatTokenClientTest {

    private static final Instant NOW = Instant.parse("2026-07-14T05:00:00Z");

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void requestsAccessTokenAndCachesIt() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> rqUid = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        startServer(200, tokenResponse("token-1", NOW.plusSeconds(300)), Duration.ZERO,
                authorization, rqUid, body, calls);

        GigaChatTokenClient client = client(Duration.ofSeconds(1));

        assertThat(client.accessToken()).isEqualTo("token-1");
        assertThat(client.accessToken()).isEqualTo("token-1");
        assertThat(calls).hasValue(1);
        assertThat(authorization.get()).isEqualTo("Basic test-authorization-key");
        assertThat(rqUid.get()).isNotBlank();
        assertThat(body.get()).isEqualTo("scope=GIGACHAT_API_PERS");
    }

    @Test
    void refreshesExpiredToken() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        startServer(200, tokenResponse("token-expired", NOW.plusSeconds(10)), Duration.ZERO,
                new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>(), calls);

        GigaChatTokenClient client = client(Duration.ofSeconds(1));

        assertThat(client.accessToken()).isEqualTo("token-expired");
        assertThat(client.accessToken()).isEqualTo("token-expired");
        assertThat(calls).hasValue(2);
    }

    @Test
    void mapsOAuthHttpError() throws IOException {
        startServer(401, "{\"message\":\"unauthorized\"}", Duration.ZERO,
                new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>(), new AtomicInteger());

        assertThatThrownBy(() -> client(Duration.ofSeconds(1)).accessToken())
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat OAuth request failed")
                .satisfies(exception -> assertThat(exception.getMessage())
                        .doesNotContain("test-authorization-key"));
    }

    @Test
    void mapsOAuthTimeout() throws IOException {
        startServer(200, tokenResponse("token-1", NOW.plusSeconds(300)), Duration.ofMillis(500),
                new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>(), new AtomicInteger());

        assertThatThrownBy(() -> client(Duration.ofMillis(50)).accessToken())
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat OAuth is unavailable");
    }

    @Test
    void rejectsInvalidOAuthPayload() {
        GigaChatTokenClient client = clientWithoutServer();

        assertThatThrownBy(() -> client.parseToken("{\"expires_at\":1}"))
                .isInstanceOf(AiListingSearchException.class)
                .hasMessage("GigaChat OAuth response does not contain access token");
    }

    private GigaChatTokenClient client(Duration timeout) {
        GigaChatProperties properties = properties(timeout);
        properties.setOauthUrl(URI.create("http://localhost:" + server.getAddress().getPort() + "/oauth"));
        return new GigaChatTokenClient(properties, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private GigaChatTokenClient clientWithoutServer() {
        return new GigaChatTokenClient(properties(Duration.ofSeconds(1)), new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private GigaChatProperties properties(Duration timeout) {
        GigaChatProperties properties = new GigaChatProperties();
        properties.setAuthorizationKey("test-authorization-key");
        properties.setTimeout(timeout);
        properties.setTokenSafetySkew(Duration.ofSeconds(30));
        properties.setScope("GIGACHAT_API_PERS");
        return properties;
    }

    private void startServer(
            int status,
            String response,
            Duration delay,
            AtomicReference<String> authorization,
            AtomicReference<String> rqUid,
            AtomicReference<String> body,
            AtomicInteger calls
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/oauth", exchange -> {
            calls.incrementAndGet();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            rqUid.set(exchange.getRequestHeaders().getFirst("RqUID"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
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

    private String tokenResponse(String token, Instant expiresAt) {
        return """
                {
                  "access_token": "%s",
                  "expires_at": %d
                }
                """.formatted(token, expiresAt.toEpochMilli());
    }
}
