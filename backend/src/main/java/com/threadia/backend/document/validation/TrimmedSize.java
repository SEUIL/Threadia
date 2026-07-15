package com.threadia.backend.document.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = TrimmedSizeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TrimmedSize {
	String message() default "trimmed length must be at most {max}";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
	int max();
}
