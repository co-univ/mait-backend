package com.coniv.mait.web.solve.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BlankAcceptedAnswerPatch(
	@NotNull @Positive Long blankNumber,
	@NotEmpty List<@Valid BlankAnswerCandidate> answersToAdd
) {
	public record BlankAnswerCandidate(
		@NotBlank String value,
		boolean main
	) {
	}
}

