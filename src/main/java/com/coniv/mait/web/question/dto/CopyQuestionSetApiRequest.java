package com.coniv.mait.web.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CopyQuestionSetApiRequest(

	@Schema(description = "복제본을 생성할 팀 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "복제할 팀 정보는 필수 입니다.")
	Long targetTeamId
) {
}
