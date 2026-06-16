package com.coniv.mait.domain.question.dto;

import com.coniv.mait.domain.question.enums.QuestionSetCommandType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@SuperBuilder
public class QuestionSetParticipantCountMessage extends QuestionSetStatusMessage {

	private long count;

	public static QuestionSetParticipantCountMessage of(Long questionSetId, long count) {
		return QuestionSetParticipantCountMessage.builder()
			.questionSetId(questionSetId)
			.commandType(QuestionSetCommandType.PARTICIPANT_COUNT)
			.count(count)
			.build();
	}
}
