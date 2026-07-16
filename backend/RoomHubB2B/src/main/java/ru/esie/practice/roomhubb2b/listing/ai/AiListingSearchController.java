package ru.esie.practice.roomhubb2b.listing.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;

import java.util.List;

@RestController
public class AiListingSearchController {

    private final AiListingSearchService service;

    public AiListingSearchController(AiListingSearchService service) {
        this.service = service;
    }

    @PostMapping("/api/listings/ai-search")
    @Operation(operationId = "searchListingsWithAi", summary = "Search listings from a natural-language prompt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching published listings",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ListingResponseDto.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid AI search request",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "GigaChat integration failed",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<ListingResponseDto> search(@Valid @RequestBody AiListingSearchRequestDto request) {
        return service.search(request).results();
    }
}
