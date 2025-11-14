package com.coniv.mait.web.question.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionSetApiResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,
	@Schema(description = "문제 셋에서 다루는 주제", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String subject,
	@Schema(description = "문제 셋 제목", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String title,
	@Schema(description = "문제 셋 생성 유형", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	QuestionSetCreationType creationType,
	@Schema(description = "문제 셋 노출 단위", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	QuestionSetVisibility visibility,
	@Schema(description = "문제 모드", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	DeliveryMode deliveryMode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	Long teamId,

	@Schema(description = "문제 셋 진행 상태", requiredMode = Schema.RequiredMode.NOT_REQUIRED, enumAsRef = true)
	QuestionSetOngoingStatus ongoingStatus,

	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionCount,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String difficulty,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDateTime updatedAt

) {
	public static QuestionSetApiResponse from(final QuestionSetDto questionSetDto) {
		return QuestionSetApiResponse.builder()
			.id(questionSetDto.getId())
			.subject(questionSetDto.getSubject())
			.title(questionSetDto.getTitle())
			.creationType(questionSetDto.getCreationType())
			.visibility(questionSetDto.getVisibility())
			.deliveryMode(questionSetDto.getDeliveryMode())
			.ongoingStatus(questionSetDto.getOngoingStatus())
			.teamId(questionSetDto.getTeamId())
			.questionCount(questionSetDto.getQuestionCount())
			.difficulty(questionSetDto.getDifficulty())
			.updatedAt(questionSetDto.getUpdatedAt())
			.build();
	}
}
