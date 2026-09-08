package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionSetSolveModeApiRequest(
	@Schema(description = "변경할 풀이 방식", enumAsRef = true, examples = {"STUDY", "LIVE_TIME"},
		requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "문제 풀이 방식을 선택해주세요.")
	QuestionSetSolveMode solveMode
) {
}
