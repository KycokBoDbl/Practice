package ru.esie.practice.roomhubb2b.listing.ai;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

final class GigaChatRestClientFactory {

    private GigaChatRestClientFactory() {
    }

    static RestClient create(GigaChatProperties properties) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(properties.timeout());

        if (properties.insecureSkipTlsVerification()) {
            builder.sslContext(insecureSslContext());
        }

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(builder.build());
        requestFactory.setReadTimeout(properties.timeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private static SSLContext insecureSslContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    null,
                    new TrustManager[]{new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }},
                    new SecureRandom()
            );
            return sslContext;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to initialize insecure GigaChat SSL context", exception);
        }
    }
}
