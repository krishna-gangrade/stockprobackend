package com.stockpro.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailAddressValidator implements ConstraintValidator<ValidEmailAddress, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.isBlank() || value.contains("..")) {
            return false;
        }

        int atIndex = value.indexOf('@');
        if (atIndex <= 0 || atIndex != value.lastIndexOf('@') || atIndex == value.length() - 1) {
            return false;
        }

        String localPart = value.substring(0, atIndex);
        String domainPart = value.substring(atIndex + 1);

        return isValidLocalPart(localPart) && isValidDomain(domainPart);
    }

    private boolean isValidLocalPart(String localPart) {
        if (localPart.isEmpty() || localPart.charAt(0) == '.' || localPart.charAt(localPart.length() - 1) == '.') {
            return false;
        }

        for (int index = 0; index < localPart.length(); index++) {
            char current = localPart.charAt(index);
            if (!isAlphaNumeric(current) && current != '_' && current != '%' && current != '+' && current != '-'
                    && current != '.') {
                return false;
            }
        }

        return true;
    }

    private boolean isValidDomain(String domainPart) {
        String[] labels = domainPart.split("\\.");
        if (labels.length < 2) {
            return false;
        }

        for (String label : labels) {
            if (label.isEmpty()) {
                return false;
            }

            for (int index = 0; index < label.length(); index++) {
                if (!isAsciiLetter(label.charAt(index))) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isAlphaNumeric(char value) {
        return isAsciiLetter(value) || (value >= '0' && value <= '9');
    }

    private boolean isAsciiLetter(char value) {
        return (value >= 'A' && value <= 'Z') || (value >= 'a' && value <= 'z');
    }
}
