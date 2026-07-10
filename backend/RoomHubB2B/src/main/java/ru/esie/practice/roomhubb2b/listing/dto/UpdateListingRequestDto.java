package ru.esie.practice.roomhubb2b.listing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.esie.practice.roomhubb2b.listing.SpaceType;

import java.math.BigDecimal;

@Schema(description = "Commercial space listing update request")
public record UpdateListingRequestDto(
        @NotBlank
        @Size(max = 255)
        @Schema(example = "Updated meeting room", maxLength = 255)
        String title,

        @Schema(example = "Screen, flip chart and coffee area", nullable = true)
        String description,

        @NotBlank
        @Size(max = 100)
        @Schema(example = "Barnaul", maxLength = 100)
        String city,

        @NotBlank
        @Size(max = 255)
        @Schema(example = "Lenina Avenue, 10", maxLength = 255)
        String address,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 8, fraction = 2)
        @Schema(example = "3000.00", minimum = "0.01")
        BigDecimal pricePerHour,

        @NotNull
        @Positive
        @Schema(example = "24", minimum = "1")
        Integer capacity,

        @NotNull
        @Schema(example = "MEETING_ROOM")
        SpaceType spaceType,

        @Pattern(regexp = "^https?://\\S+$", message = "must be an HTTP(S) URL")
        @Schema(example = "https://example.com/listing-42.jpg", nullable = true)
        String imageUrl
) {
}
