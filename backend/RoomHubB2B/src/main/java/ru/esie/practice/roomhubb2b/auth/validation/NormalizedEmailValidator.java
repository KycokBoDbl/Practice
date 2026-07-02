package ru.esie.practice.roomhubb2b.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class NormalizedEmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || EMAIL_PATTERN.matcher(value.trim()).matches();
    }
}
