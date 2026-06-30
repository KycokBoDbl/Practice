package ru.esie.practice.roomhubb2b.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI roomHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RoomHub B2B API")
                        .version("1.0.0")
                        .description("REST API for the RoomHub B2B commercial space marketplace."))
                .servers(List.of(new Server()
                        .url("/")
                        .description("Current RoomHub B2B server")));
    }
}
