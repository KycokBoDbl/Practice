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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.esie.practice.roomhubb2b.listing.dto.CreateListingRequestDto;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;
import ru.esie.practice.roomhubb2b.listing.dto.OwnedListingResponseDto;
import ru.esie.practice.roomhubb2b.listing.dto.UpdateListingRequestDto;

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

    @GetMapping("/api/listings/owned")
    @Operation(operationId = "getOwnedListings", summary = "List owned commercial space listings")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Owned listings",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OwnedListingResponseDto.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only landlords can list owned listings",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<OwnedListingResponseDto> getOwnedListings(@AuthenticationPrincipal Jwt jwt) {
        return listingService.getOwnedListings(ListingActor.from(jwt));
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

    @PutMapping("/api/listings/{listingId}")
    @Operation(operationId = "updateListing", summary = "Update an owned commercial space listing")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ListingResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid listing request",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only landlords can manage listings",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ListingResponseDto updateListing(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long listingId,
            @Valid @RequestBody UpdateListingRequestDto request
    ) {
        return listingService.update(ListingActor.from(jwt), listingId, request);
    }

    @PostMapping("/api/listings/{listingId}/hide")
    @Operation(operationId = "hideListing", summary = "Hide an owned commercial space listing")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Listing hidden"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only landlords can manage listings",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> hideListing(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long listingId
    ) {
        listingService.hide(ListingActor.from(jwt), listingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/listings/{listingId}/activate")
    @Operation(operationId = "activateListing", summary = "Reactivate an owned commercial space listing")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Listing activated"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only landlords can manage listings",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> activateListing(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long listingId
    ) {
        listingService.activate(ListingActor.from(jwt), listingId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/listings/{listingId}")
    @Operation(operationId = "deleteListing", summary = "Delete an owned commercial space listing")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Listing deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Only landlords can manage listings",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Listing has booking history",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteListing(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long listingId
    ) {
        listingService.delete(ListingActor.from(jwt), listingId);
        return ResponseEntity.noContent().build();
    }
}
