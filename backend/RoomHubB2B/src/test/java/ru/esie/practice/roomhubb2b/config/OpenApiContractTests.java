package ru.esie.practice.roomhubb2b.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesListingsInOpenApiContract() throws Exception {
        mockMvc.perform(get("/api/openapi").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("RoomHub B2B API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andExpect(jsonPath("$['paths']['/api/listings']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/listings']['get']['responses']['200']['content']['application/json']['schema']['type']").value("array"))
                .andExpect(jsonPath("$['paths']['/api/listings']['get']['responses']['200']['content']['application/json']['schema']['items']['$ref']").value("#/components/schemas/ListingResponseDto"))
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.id").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.title").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.city").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.pricePerHour").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.capacity").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.spaceType").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.imageUrl").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.description").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.address").exists());
    }

    @Test
    void exposesHourlyListingAvailabilityInOpenApiContract() throws Exception {
        String operation = "$['paths']['/api/listings/{listingId}/availability']['get']";

        mockMvc.perform(get("/api/openapi").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(operation + ".operationId").value("getListingAvailability"))
                .andExpect(jsonPath(operation + ".parameters[?(@.name == 'listingId')]").exists())
                .andExpect(jsonPath(operation + ".parameters[?(@.name == 'from')]").exists())
                .andExpect(jsonPath(operation + ".parameters[?(@.name == 'to')]").exists())
                .andExpect(jsonPath(operation + ".responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ListingAvailabilityResponseDto"))
                .andExpect(jsonPath(operation + ".responses['400']").exists())
                .andExpect(jsonPath(operation + ".responses['404']").exists())
                .andExpect(jsonPath("$.components.schemas.ListingAvailabilityResponseDto.properties.listingId").exists())
                .andExpect(jsonPath("$.components.schemas.ListingAvailabilityResponseDto.properties.from.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.ListingAvailabilityResponseDto.properties.from.format")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ListingAvailabilityResponseDto.properties.to.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.ListingAvailabilityResponseDto.properties.busyIntervals.items['$ref']")
                        .value("#/components/schemas/BusyIntervalResponseDto"))
                .andExpect(jsonPath("$.components.schemas.BusyIntervalResponseDto.properties.startAt.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.BusyIntervalResponseDto.properties.startAt.format")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.BusyIntervalResponseDto.properties.endAt.type")
                        .value("string"));
    }

    @Test
    void keepsListingsJsonFieldNamesUnchanged() throws Exception {
        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].length()").value(9))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].city").exists())
                .andExpect(jsonPath("$[0].pricePerHour").exists())
                .andExpect(jsonPath("$[0].capacity").exists())
                .andExpect(jsonPath("$[0].spaceType").exists())
                .andExpect(jsonPath("$[0].imageUrl").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].address").exists());
    }
}
