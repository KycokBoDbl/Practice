package ru.esie.practice.roomhubb2b.listing.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Natural-language listing search prompt")
public record AiListingSearchRequestDto(
        @Schema(
                description = "Natural-language prompt describing desired listing filters",
                example = "Need a conference hall in Barnaul for 30 people under 5000 per hour",
                minLength = 1,
                maxLength = MAX_PROMPT_LENGTH
        )
        @NotBlank
        @Size(max = MAX_PROMPT_LENGTH)
        String prompt
) {
    public static final int MAX_PROMPT_LENGTH = 1000;
}
