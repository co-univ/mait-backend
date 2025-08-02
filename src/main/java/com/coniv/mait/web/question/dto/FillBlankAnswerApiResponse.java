package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;

public record FillBlankAnswerApiResponse(
	String answer,
	boolean isMain,
	Long number
) {
	public static FillBlankAnswerApiResponse from(FillBlankAnswerDto dto) {
		return new FillBlankAnswerApiResponse(
			dto.getAnswer(),
			dto.isMain(),
			dto.getNumber()
		);
	}
}