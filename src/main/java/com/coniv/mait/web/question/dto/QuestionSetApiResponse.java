package com.coniv.mait.web.question.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionSetApiResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,
	String subject,
	String title,
	@Schema(description = "문제 셋 생성 유형", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	QuestionSetCreationType creationType,
	@Schema(description = "문제 셋 노출 단위", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	QuestionSetVisibility visibility,
	@Schema(description = "문제 모드", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	DeliveryMode deliveryMode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	Long teamId,

	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionCount,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String levelDescription,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDateTime createdAt

) {
	public static QuestionSetApiResponse from(final QuestionSetDto questionSetDto) {
		return QuestionSetApiResponse.builder()
			.id(questionSetDto.getId())
			.subject(questionSetDto.getSubject())
			.title(questionSetDto.getTitle())
			.creationType(questionSetDto.getCreationType())
			.visibility(questionSetDto.getVisibility())
			.deliveryMode(questionSetDto.getDeliveryMode())
			.teamId(questionSetDto.getTeamId())
			.questionCount(questionSetDto.getQuestionCount())
			.levelDescription(questionSetDto.getLevelDescription())
			.createdAt(questionSetDto.getCreatedAt())
			.build();
	}
}
