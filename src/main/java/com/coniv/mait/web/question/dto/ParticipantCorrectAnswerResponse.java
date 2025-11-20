package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.ParticipantCorrectAnswersDto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParticipantCorrectAnswerResponse {
	ParticipantInfoApiResponse participantInfos;
	long correctAnswerCount;

	public static ParticipantCorrectAnswerResponse from(ParticipantCorrectAnswersDto participantCorrectAnswersDto) {
		return new ParticipantCorrectAnswerResponse(
			ParticipantInfoApiResponse.from(participantCorrectAnswersDto.getParticipantDto()),
			participantCorrectAnswersDto.getCorrectAnswerCount()
		);
	}
}
