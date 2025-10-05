package com.coniv.mait.web.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateQuestionOrderApiRequest(
	@Schema(description = "바로 앞에 올 문제 ID (없으면 null)")
	Long prevQuestionId,
	@Schema(description = "바로 뒤에 올 문제 ID (없으면 null)")
	Long nextQuestionId
) {
}
