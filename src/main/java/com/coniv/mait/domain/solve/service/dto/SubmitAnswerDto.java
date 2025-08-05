package com.coniv.mait.domain.solve.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

public interface SubmitAnswerDto<T> {

	QuestionType getType();

	List<T> getSubmitAnswers();
}
