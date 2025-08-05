package com.coniv.mait.domain.solve.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

import io.swagger.v3.oas.annotations.media.Schema;

public interface SubmitAnswerDto<T> {

	@Schema(enumAsRef = true)
	QuestionType getType();

	List<T> getSubmitAnswers();
}
