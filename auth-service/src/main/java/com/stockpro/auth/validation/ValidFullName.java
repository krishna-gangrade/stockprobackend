package com.stockpro.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = FullNameValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface ValidFullName {
    String message() default "Full name can contain letters and spaces only";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
