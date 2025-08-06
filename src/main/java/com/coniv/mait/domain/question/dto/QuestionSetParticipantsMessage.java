package com.coniv.mait.domain.question.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@SuperBuilder
public class QuestionSetParticipantsMessage extends QuestionSetStatusMessage {
	private List<ParticipantDto> activeParticipants;
}
