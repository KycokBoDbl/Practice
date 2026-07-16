package ru.esie.practice.roomhubb2b.listing.ai;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;

import java.util.List;

@Schema(description = "AI listing search result with interpreted filters")
public record AiListingSearchResponseDto(
        @Schema(description = "Structured hard filters applied by backend")
        AiListingSearchInterpretedFilterDto interpretedFilter,
        @ArraySchema(arraySchema = @Schema(
                description = "Prompt terms understood by the model but not applied as hard SQL filters"))
        List<String> ignoredTerms,
        @ArraySchema(arraySchema = @Schema(description = "Matching published listings"))
        List<ListingResponseDto> results
) {
}
