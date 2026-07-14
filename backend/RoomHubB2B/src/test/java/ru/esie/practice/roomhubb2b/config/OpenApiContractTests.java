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
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.spaceType.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.spaceType.enum.length()")
                        .value(5))
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.spaceType.enum")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                "MEETING_ROOM",
                                "CONFERENCE_HALL",
                                "CLASSROOM",
                                "LOFT",
                                "SHOWROOM"
                        )))
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.imageUrl").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.description").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.address").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.ownerOrganizationName").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.latitude").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.longitude").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.ownerOrganizationId")
                        .doesNotExist());
    }

    @Test
    void exposesProtectedListingPublicationContract() throws Exception {
        String publish = "$['paths']['/api/listings']['post']";

        mockMvc.perform(get("/api/openapi").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(publish + ".operationId").value("publishListing"))
                .andExpect(jsonPath(publish + ".security[0].bearerAuth").isArray())
                .andExpect(jsonPath(publish + ".requestBody.required").value(true))
                .andExpect(jsonPath(publish + ".requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/CreateListingRequestDto"))
                .andExpect(jsonPath(publish + ".responses['201'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ListingResponseDto"))
                .andExpect(jsonPath(publish + ".responses['201'].headers.Location.schema.type")
                        .value("string"))
                .andExpect(jsonPath(publish + ".responses['201'].headers.Location.schema.format")
                        .value("uri"))
                .andExpect(jsonPath(publish + ".responses['400']").exists())
                .andExpect(jsonPath(publish + ".responses['401']").exists())
                .andExpect(jsonPath(publish + ".responses['403']").exists())
                .andExpect(jsonPath(publish + ".responses['400'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath(publish + ".responses['401'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath(publish + ".responses['403'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath("$.components.schemas.CreateListingRequestDto.required")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                "title", "city", "address", "pricePerHour", "capacity", "spaceType"
                        )))
                .andExpect(jsonPath("$.components.schemas.CreateListingRequestDto.properties.ownerOrganizationId")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.CreateListingRequestDto.properties.status")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.CreateListingRequestDto.properties.createdAt")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.CreateListingRequestDto.properties.latitude")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.CreateListingRequestDto.properties.longitude")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.description.type")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("string", "null")))
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.imageUrl.type")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("string", "null")))
                .andExpect(jsonPath("$['paths']['/api/listings']['get'].security").doesNotExist());
    }

    @Test
    void exposesProtectedListingManagementContract() throws Exception {
        String listing = "$['paths']['/api/listings/{listingId}']";
        String update = listing + "['put']";
        String delete = listing + "['delete']";
        String hide = "$['paths']['/api/listings/{listingId}/hide']['post']";
        String activate = "$['paths']['/api/listings/{listingId}/activate']['post']";
        String owned = "$['paths']['/api/listings/owned']['get']";

        mockMvc.perform(get("/api/openapi").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(owned + ".operationId").value("getOwnedListings"))
                .andExpect(jsonPath(owned + ".security[0].bearerAuth").isArray())
                .andExpect(jsonPath(owned + ".responses['200'].content['application/json'].schema.type")
                        .value("array"))
                .andExpect(jsonPath(owned + ".responses['200'].content['application/json'].schema.items['$ref']")
                        .value("#/components/schemas/OwnedListingResponseDto"))
                .andExpect(jsonPath(owned + ".responses['401'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath(owned + ".responses['403'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath(update + ".operationId").value("updateListing"))
                .andExpect(jsonPath(update + ".security[0].bearerAuth").isArray())
                .andExpect(jsonPath(update + ".parameters[?(@.name == 'listingId')]").exists())
                .andExpect(jsonPath(update + ".requestBody.required").value(true))
                .andExpect(jsonPath(update + ".requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/UpdateListingRequestDto"))
                .andExpect(jsonPath(update + ".responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ListingResponseDto"))
                .andExpect(jsonPath(update + ".responses['400']").exists())
                .andExpect(jsonPath(update + ".responses['401']").exists())
                .andExpect(jsonPath(update + ".responses['403']").exists())
                .andExpect(jsonPath(update + ".responses['404']").exists())
                .andExpect(jsonPath(hide + ".operationId").value("hideListing"))
                .andExpect(jsonPath(hide + ".security[0].bearerAuth").isArray())
                .andExpect(jsonPath(hide + ".parameters[?(@.name == 'listingId')]").exists())
                .andExpect(jsonPath(hide + ".responses['204']").exists())
                .andExpect(jsonPath(hide + ".responses['401']").exists())
                .andExpect(jsonPath(hide + ".responses['403']").exists())
                .andExpect(jsonPath(hide + ".responses['404']").exists())
                .andExpect(jsonPath(activate + ".operationId").value("activateListing"))
                .andExpect(jsonPath(activate + ".security[0].bearerAuth").isArray())
                .andExpect(jsonPath(activate + ".parameters[?(@.name == 'listingId')]").exists())
                .andExpect(jsonPath(activate + ".responses['204']").exists())
                .andExpect(jsonPath(activate + ".responses['401']").exists())
                .andExpect(jsonPath(activate + ".responses['403']").exists())
                .andExpect(jsonPath(activate + ".responses['404']").exists())
                .andExpect(jsonPath(delete + ".operationId").value("deleteListing"))
                .andExpect(jsonPath(delete + ".security[0].bearerAuth").isArray())
                .andExpect(jsonPath(delete + ".parameters[?(@.name == 'listingId')]").exists())
                .andExpect(jsonPath(delete + ".responses['204']").exists())
                .andExpect(jsonPath(delete + ".responses['401']").exists())
                .andExpect(jsonPath(delete + ".responses['403']").exists())
                .andExpect(jsonPath(delete + ".responses['404']").exists())
                .andExpect(jsonPath(delete + ".responses['409']").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateListingRequestDto.required")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                "title", "city", "address", "pricePerHour", "capacity", "spaceType"
                        )))
                .andExpect(jsonPath("$.components.schemas.UpdateListingRequestDto.properties.title").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateListingRequestDto.properties.pricePerHour").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateListingRequestDto.properties.spaceType").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateListingRequestDto.properties.ownerOrganizationId")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.UpdateListingRequestDto.properties.status")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.UpdateListingRequestDto.properties.createdAt")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.UpdateListingRequestDto.properties.latitude")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.UpdateListingRequestDto.properties.longitude")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.ownerOrganizationName").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.latitude").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.longitude").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.ownerOrganizationId")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.id").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.title").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.city").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.pricePerHour").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.capacity").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.spaceType").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.imageUrl").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.description").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.address").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.ownerOrganizationName")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.latitude").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.longitude").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.status").exists())
                .andExpect(jsonPath("$.components.schemas.OwnedListingResponseDto.properties.ownerOrganizationId")
                        .doesNotExist());
    }

    @Test
    void exposesAiListingSearchContract() throws Exception {
        String operation = "$['paths']['/api/listings/ai-search']['post']";

        mockMvc.perform(get("/api/openapi").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(operation + ".operationId").value("searchListingsWithAi"))
                .andExpect(jsonPath(operation + ".security").doesNotExist())
                .andExpect(jsonPath(operation + ".requestBody.required").value(true))
                .andExpect(jsonPath(operation + ".requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/AiListingSearchRequestDto"))
                .andExpect(jsonPath(operation + ".responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/AiListingSearchResponseDto"))
                .andExpect(jsonPath(operation + ".responses['400'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath(operation + ".responses['502'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath("$.components.schemas.AiListingSearchRequestDto.required")
                        .value(org.hamcrest.Matchers.contains("prompt")))
                .andExpect(jsonPath("$.components.schemas.AiListingSearchRequestDto.properties.prompt").exists())
                .andExpect(jsonPath("$.components.schemas.AiListingSearchRequestDto.properties.prompt.maxLength")
                        .value(1000))
                .andExpect(jsonPath("$.components.schemas.AiListingSearchRequestDto.properties.authorizationKey")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.AiListingSearchRequestDto.properties.accessToken")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.AiListingSearchRequestDto.properties.spaceType")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.AiListingSearchResponseDto.properties.interpretedFilter")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.AiListingSearchResponseDto.properties.ignoredTerms.items.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.AiListingSearchResponseDto.properties.results.items['$ref']")
                        .value("#/components/schemas/ListingResponseDto"))
                .andExpect(jsonPath("$.components.schemas.AiListingSearchInterpretedFilterDto.properties.city")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.AiListingSearchInterpretedFilterDto.properties.spaceType")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.AiListingSearchInterpretedFilterDto.properties.minCapacity")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.AiListingSearchInterpretedFilterDto.properties.minPricePerHour")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.AiListingSearchInterpretedFilterDto.properties.maxPricePerHour")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.AiListingSearchInterpretedFilterDto.properties.availableFrom.type")
                        .value(org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.is("string"),
                                org.hamcrest.Matchers.hasItem("string")
                        )))
                .andExpect(jsonPath("$.components.schemas.AiListingSearchInterpretedFilterDto.properties.availableTo.type")
                        .value(org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.is("string"),
                                org.hamcrest.Matchers.hasItem("string")
                        )))
                .andExpect(jsonPath("$['paths']['/api/listings']['get'].security").doesNotExist());
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
                .andExpect(jsonPath("$[0].length()").value(12))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].city").exists())
                .andExpect(jsonPath("$[0].pricePerHour").exists())
                .andExpect(jsonPath("$[0].capacity").exists())
                .andExpect(jsonPath("$[?(@.spaceType == 'MEETING_ROOM')]").exists())
                .andExpect(jsonPath("$[0].imageUrl").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].address").exists())
                .andExpect(jsonPath("$[0].latitude").hasJsonPath())
                .andExpect(jsonPath("$[0].longitude").hasJsonPath());
    }

    @Test
    void exposesAuthenticationContractAndSecurityBoundaries() throws Exception {
        String register = "$['paths']['/api/auth/register']['post']";
        String login = "$['paths']['/api/auth/login']['post']";
        String me = "$['paths']['/api/auth/me']['get']";

        mockMvc.perform(get("/api/openapi").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(register).exists())
                .andExpect(jsonPath(register + ".security").doesNotExist())
                .andExpect(jsonPath(register + ".responses['400']").exists())
                .andExpect(jsonPath(register + ".responses['409']").exists())
                .andExpect(jsonPath(login).exists())
                .andExpect(jsonPath(login + ".security").doesNotExist())
                .andExpect(jsonPath(login + ".responses['400']").exists())
                .andExpect(jsonPath(login + ".responses['401']").exists())
                .andExpect(jsonPath(me).exists())
                .andExpect(jsonPath(me + ".security[0].bearerAuth").isArray())
                .andExpect(jsonPath(me + ".responses['401']").exists())
                .andExpect(jsonPath(me + ".responses['403']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.schemas.RegisterRequestDto.properties.password.writeOnly").value(true))
                .andExpect(jsonPath("$.components.schemas.RegisterRequestDto.properties.password.minLength").value(8))
                .andExpect(jsonPath("$.components.schemas.ProfileResponseDto.properties.passwordHash").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/listings']['get'].security").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/listings/{listingId}/availability']['get'].security").doesNotExist());
    }

    @Test
    void exposesProtectedBookingWorkflowContract() throws Exception {
        String create = "$['paths']['/api/bookings']['post']";
        String booking = "$['paths']['/api/bookings/{bookingId}']['get']";
        String history = "$['paths']['/api/bookings/{bookingId}/history']['get']";

        mockMvc.perform(get("/api/openapi").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(create + ".security[0].bearerAuth").isArray())
                .andExpect(jsonPath(create + ".requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/CreateBookingRequestDto"))
                .andExpect(jsonPath(create + ".responses['201']").exists())
                .andExpect(jsonPath(create + ".responses['400']").exists())
                .andExpect(jsonPath(create + ".responses['401']").exists())
                .andExpect(jsonPath(create + ".responses['403']").exists())
                .andExpect(jsonPath(create + ".responses['404']").exists())
                .andExpect(jsonPath(create + ".responses['409']").exists())
                .andExpect(jsonPath(booking).exists())
                .andExpect(jsonPath(history).exists())
                .andExpect(jsonPath("$['paths']['/api/bookings/{bookingId}/approve']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/bookings/{bookingId}/reject']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/bookings/{bookingId}/confirm']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/bookings/{bookingId}/cancel']['post']").exists())
                .andExpect(jsonPath("$.components.schemas.CreateBookingRequestDto.properties.tenantOrganizationId")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.ownerOrganizationName").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.latitude").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.longitude").exists())
                .andExpect(jsonPath("$.components.schemas.ListingResponseDto.properties.ownerOrganizationId")
                        .doesNotExist());
    }
}
