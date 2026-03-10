package com.coniv.mait.domain.solve.service.dto;

import java.util.Objects;

import com.coniv.mait.domain.solve.entity.StudyAnswerDraftEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudyAnswerDraftDto {
	private Long solvingSessionId;
	private Long questionId;
	private String submittedAnswer;
	private boolean submitted;

	public static StudyAnswerDraftDto from(StudyAnswerDraftEntity draft) {
		return StudyAnswerDraftDto.builder()
			.solvingSessionId(Objects.requireNonNull(draft.getId()).getSolvingSessionId())
			.questionId(draft.getId().getQuestionId())
			.submittedAnswer(draft.getSubmittedAnswer())
			.submitted(draft.isSubmitted())
			.build();
	}
}
