package com.stockpro.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.length() < 8) {
            return false;
        }

        boolean hasLowercase = false;
        boolean hasUppercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);

            if (current >= 'a' && current <= 'z') {
                hasLowercase = true;
            } else if (current >= 'A' && current <= 'Z') {
                hasUppercase = true;
            } else if (current >= '0' && current <= '9') {
                hasDigit = true;
            } else if (isSupportedSpecialCharacter(current)) {
                hasSpecial = true;
            } else {
                return false;
            }
        }

        return hasLowercase && hasUppercase && hasDigit && hasSpecial;
    }

    private boolean isSupportedSpecialCharacter(char value) {
        return value == '@' || value == '$' || value == '!' || value == '%' || value == '*'
                || value == '?' || value == '&';
    }
}
