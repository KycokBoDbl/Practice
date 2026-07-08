package ru.esie.practice.roomhubb2b.listing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.esie.practice.roomhubb2b.listing.dto.CreateListingRequestDto;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;

import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Listings", description = "Published commercial space listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/api/listings")
    @Operation(summary = "List published commercial spaces")
    @ApiResponse(
            responseCode = "200",
            description = "Published listings",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ListingResponseDto.class))
            )
    )
    public List<ListingResponseDto> getListings() {
        return listingService.getPublishedListings();
    }

    @PostMapping("/api/listings")
    @Operation(operationId = "publishListing", summary = "Publish a commercial space listing")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Listing published",
                    headers = @Header(name = "Location", description = "Published listing URI",
                            schema = @Schema(type = "string", format = "uri")),
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ListingResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid listing request",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only landlords can publish listings",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ListingResponseDto> publishListing(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateListingRequestDto request
    ) {
        ListingResponseDto listing = listingService.publish(ListingActor.from(jwt), request);
        return ResponseEntity.created(URI.create("/api/listings/" + listing.id())).body(listing);
    }
}
