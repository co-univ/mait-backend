package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;

import jakarta.validation.Valid;

public record BaseUpdateQuestionApiRequest(
	@Valid
	List<MultipleQuestionDto> multipleQuestions
) {
}
