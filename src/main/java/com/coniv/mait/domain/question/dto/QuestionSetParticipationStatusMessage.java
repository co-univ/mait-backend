package com.coniv.mait.domain.question.dto;

import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionStatusType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSetParticipationStatusMessage {

	private Long questionSetId;
	private ParticipantStatus participantStatus;
	private Long questionId;
	private QuestionStatusType statusType;
}
