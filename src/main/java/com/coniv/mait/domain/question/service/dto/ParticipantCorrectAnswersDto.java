package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.dto.ParticipantDto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParticipantCorrectAnswersDto {
	ParticipantDto participantDto;
	long correctAnswerCount;

	public static ParticipantCorrectAnswersDto from(ParticipantDto participantDto, long correctAnswerCount) {
		return new ParticipantCorrectAnswersDto(participantDto, correctAnswerCount);
	}
}
