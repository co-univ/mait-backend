package com.coniv.mait.domain.question.dto;

import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionStatusType;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuestionSetInitialStateMessage {

	private Long questionSetId;
	private ParticipantStatus participantStatus;
	private Long questionId;
	private QuestionStatusType statusType;

	@Builder
	public QuestionSetInitialStateMessage(Long questionSetId, ParticipantStatus participantStatus,
		Long questionId, QuestionStatusType statusType) {
		this.questionSetId = questionSetId;
		this.participantStatus = participantStatus;
		this.questionId = questionId;
		this.statusType = statusType;
	}
}
