package com.coniv.mait.domain.solve.service.component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

public interface AnswerChecker<T> {

	QuestionType getQuestionType();

	boolean checkAnswer(final QuestionEntity question, final SubmitAnswerDto<T> answers);
}
