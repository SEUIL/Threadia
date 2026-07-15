package com.threadia.backend.document.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TrimmedSizeValidator implements ConstraintValidator<TrimmedSize, String> {
	private int max;

	@Override
	public void initialize(TrimmedSize constraintAnnotation) {
		max = constraintAnnotation.max();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || value.trim().length() <= max;
	}
}
