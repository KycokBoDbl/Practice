package ru.esie.practice.roomhubb2b.listing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;

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
}
