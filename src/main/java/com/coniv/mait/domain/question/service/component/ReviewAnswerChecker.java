package com.coniv.mait.domain.question.service.component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

public interface ReviewAnswerChecker<T> {
	QuestionType getQuestionType();

	ReviewAnswerCheckResult check(Long questionId, QuestionEntity question, SubmitAnswerDto<T> submitAnswers);
}
