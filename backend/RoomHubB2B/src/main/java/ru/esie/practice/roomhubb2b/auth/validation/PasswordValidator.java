package ru.esie.practice.roomhubb2b.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int characters = value.codePointCount(0, value.length());
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        return characters >= 12 && characters <= 64 && bytes <= 72;
    }
}
