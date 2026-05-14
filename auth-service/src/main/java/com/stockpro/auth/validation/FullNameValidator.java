package com.stockpro.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FullNameValidator implements ConstraintValidator<ValidFullName, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.isBlank()) {
            return false;
        }

        boolean previousWasSpace = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);

            if (current == ' ') {
                if (index == 0 || index == value.length() - 1 || previousWasSpace) {
                    return false;
                }
                previousWasSpace = true;
                continue;
            }

            if (!isAsciiLetter(current)) {
                return false;
            }

            previousWasSpace = false;
        }

        return true;
    }

    private boolean isAsciiLetter(char value) {
        return (value >= 'A' && value <= 'Z') || (value >= 'a' && value <= 'z');
    }
}
