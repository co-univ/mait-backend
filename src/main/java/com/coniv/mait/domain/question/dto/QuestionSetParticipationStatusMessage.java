package com.coniv.mait.domain.question.dto;

import com.coniv.mait.domain.question.enums.ParticipantStatus;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuestionSetParticipationStatusMessage {

	private Long questionSetId;
	private ParticipantStatus participantStatus;

	@Builder
	public QuestionSetParticipationStatusMessage(Long questionSetId, ParticipantStatus participantStatus) {
		this.questionSetId = questionSetId;
		this.participantStatus = participantStatus;
	}
}
