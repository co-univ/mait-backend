package com.coniv.mait.web.solve.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public record MultipleChoiceUpdateAnswerPayload(
	@NotEmpty Set<Long> correctChoiceIds
) implements UpdateAnswerPayload {

}

