package com.coniv.mait.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TrimmedSizeValidator implements ConstraintValidator<TrimmedSize, String> {

	private int min;
	private int max;

	@Override
	public void initialize(TrimmedSize constraintAnnotation) {
		this.min = constraintAnnotation.min();
		this.max = constraintAnnotation.max();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}

		int trimmedLength = value.trim().length();
		return trimmedLength >= min && trimmedLength <= max;
	}
}

