package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateQuestionSetApiRequest(
	@Schema(description = "문제 셋 제목")
	@NotBlank(message = "제목을 입력해주세요")
	String title,
	@Schema(description = "문제 셋 주제")
	@NotBlank(message = "주제를 입력해주세요")
	String subject,

	@Schema(description = "문제 풀이 방식", enumAsRef = true)
	DeliveryMode mode,

	@Schema(description = "문제 셋 난이도 설명")
	String difficulty,

	@Schema(description = "문제 셋 공개 단위", enumAsRef = true)
	QuestionSetVisibility visibility
) {
}
