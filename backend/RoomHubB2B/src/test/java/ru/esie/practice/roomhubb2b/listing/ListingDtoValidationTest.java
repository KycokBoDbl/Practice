package ru.esie.practice.roomhubb2b.listing;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.esie.practice.roomhubb2b.listing.dto.CreateListingRequestDto;
import ru.esie.practice.roomhubb2b.listing.dto.UpdateListingRequestDto;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ListingDtoValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidRequestWithOptionalFieldsMissing() {
        assertThat(validator.validate(validRequest(null, null))).isEmpty();
    }

    @Test
    void rejectsInvalidRequiredFieldsAndNumericBounds() {
        CreateListingRequestDto request = new CreateListingRequestDto(
                " ",
                null,
                "x".repeat(101),
                "x".repeat(256),
                new BigDecimal("100000000.00"),
                0,
                null,
                null
        );

        assertThat(fields(validator.validate(request)))
                .contains("title", "city", "address", "pricePerHour", "capacity", "spaceType");
    }

    @Test
    void rejectsNonHttpImageUrlAndExcessFractionDigits() {
        CreateListingRequestDto request = new CreateListingRequestDto(
                "Meeting room",
                "Description",
                "Barnaul",
                "Lenina Avenue, 10",
                new BigDecimal("1.001"),
                1,
                SpaceType.MEETING_ROOM,
                "ftp://example.com/image.jpg"
        );

        assertThat(fields(validator.validate(request))).containsExactlyInAnyOrder("pricePerHour", "imageUrl");
    }

    @Test
    void acceptsValidUpdateRequestWithOptionalFieldsMissing() {
        UpdateListingRequestDto request = new UpdateListingRequestDto(
                "Updated meeting room",
                null,
                "Barnaul",
                "Lenina Avenue, 10",
                new BigDecimal("3000.00"),
                24,
                SpaceType.MEETING_ROOM,
                null
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidUpdateRequiredFieldsAndNumericBounds() {
        UpdateListingRequestDto request = new UpdateListingRequestDto(
                " ",
                null,
                "x".repeat(101),
                "x".repeat(256),
                new BigDecimal("100000000.00"),
                0,
                null,
                "ftp://example.com/image.jpg"
        );

        assertThat(fields(validator.validate(request)))
                .contains("title", "city", "address", "pricePerHour", "capacity", "spaceType", "imageUrl");
    }

    private CreateListingRequestDto validRequest(String description, String imageUrl) {
        return new CreateListingRequestDto(
                "Meeting room",
                description,
                "Barnaul",
                "Lenina Avenue, 10",
                new BigDecimal("2500.00"),
                20,
                SpaceType.MEETING_ROOM,
                imageUrl
        );
    }

    private Set<String> fields(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
