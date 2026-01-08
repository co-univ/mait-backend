package com.coniv.mait.web.solve.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionAnswerApiRequest(
	@NotNull(message = "수정할 값을 입력해주세요.")
	@Valid UpdateAnswerPayload payload
) {
}
