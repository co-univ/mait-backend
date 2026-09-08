package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CopyQuestionSetApiRequest(

	@Schema(description = "복제본을 생성할 팀 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "복제할 팀 정보는 필수 입니다.")
	Long targetTeamId,

	@Schema(description = "복제본 문제 셋 제목", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "문제 셋 제목을 입력해주세요.")
	String title,

	@Schema(description = "복제본 문제 풀이 방식 (실시간/학습)", enumAsRef = true,
		examples = {"STUDY", "LIVE_TIME"}, requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "문제 풀이 방식을 선택해주세요.")
	QuestionSetSolveMode solveMode
) {
}
