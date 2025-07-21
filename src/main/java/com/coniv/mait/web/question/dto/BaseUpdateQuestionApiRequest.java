package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;

public record BaseUpdateQuestionApiRequest(
	List<MultipleQuestionDto> multipleQuestions
) {
}
