package com.coniv.mait.web.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateDefaultQuestionApiRequest(
	@Schema(description = "문제 번호")
	@NotNull(message = "문제 번호는 필수 입니다.")
	Long number
) {
}
