package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.solve.service.dto.AnswerSubmitRecordDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionAnswerSubmitRecordApiResponse(
	@Schema(description = "제출 기록 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,
	@Schema(description = "제출한 유저 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long userId,
	@Schema(description = "제출한 유저 이름", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String userName,

	@Schema(description = "제출한 유저 닉네임", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String userNickname,
	@Schema(description = "제출한 문제 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionId,
	@Schema(description = "정/오답 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean isCorrect,
	@Schema(description = "제출 순서", requiredMode = Schema.RequiredMode.REQUIRED)
	Long submitOrder

	// 제출한 정답
) {
	public static List<QuestionAnswerSubmitRecordApiResponse> from(List<AnswerSubmitRecordDto> submitRecords) {
		return submitRecords.stream()
			.map(record -> QuestionAnswerSubmitRecordApiResponse.builder()
				.id(record.getId())
				.userId(record.getUserId())
				.userName(record.getUserName())
				.userNickname(record.getUserNickname())
				.questionId(record.getQuestionId())
				.isCorrect(record.isCorrect())
				.submitOrder(record.getSubmitOrder())
				.build())
			.toList();
	}
}
