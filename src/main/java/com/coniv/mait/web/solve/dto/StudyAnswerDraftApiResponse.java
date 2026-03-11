package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.solve.service.dto.StudyAnswerDraftDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudyAnswerDraftApiResponse(
	@Schema(description = "풀이 세션 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long solvingSessionId,
	@Schema(description = "문제 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionId,
	@Schema(description = "제출한 답안", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	String submittedAnswer,
	@Schema(description = "제출 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean submitted
) {
	public static StudyAnswerDraftApiResponse from(StudyAnswerDraftDto dto) {
		return new StudyAnswerDraftApiResponse(
			dto.getSolvingSessionId(),
			dto.getQuestionId(),
			dto.getSubmittedAnswer(),
			dto.isSubmitted()
		);
	}

	public static List<StudyAnswerDraftApiResponse> from(List<StudyAnswerDraftDto> dtos) {
		return dtos.stream()
			.map(StudyAnswerDraftApiResponse::from)
			.toList();
	}
}
