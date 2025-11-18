package com.coniv.mait.web.solve.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionAnswerApiRequest(
	@NotNull @Valid UpdateAnswerPayload payload
) {
}
