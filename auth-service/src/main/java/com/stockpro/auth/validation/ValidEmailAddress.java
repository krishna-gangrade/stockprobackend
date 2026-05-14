package com.stockpro.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = EmailAddressValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface ValidEmailAddress {
    String message() default
            "Email must be valid, cannot contain consecutive dots, and must have an alphabetic domain like gmail.com";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
