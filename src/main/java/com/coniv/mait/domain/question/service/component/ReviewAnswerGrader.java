package com.coniv.mait.domain.question.service.component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionExceptionCode;
import com.coniv.mait.domain.question.exception.QuestionStatusException;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.solve.service.dto.FillBlankSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

@Component
public class ReviewAnswerGrader {

	private final Map<QuestionType, ReviewAnswerChecker<?>> checkers;

	@Autowired
	public ReviewAnswerGrader(List<ReviewAnswerChecker<?>> checkers) {
		this.checkers = checkers.stream()
			.collect(Collectors.toUnmodifiableMap(ReviewAnswerChecker::getQuestionType, Function.identity()));
	}

	@SuppressWarnings("unchecked")
	public ReviewAnswerCheckResult gradeAnswer(Long questionId, QuestionEntity question,
		SubmitAnswerDto<?> submitAnswers) {
		ReviewAnswerChecker<?> checker = checkers.get(question.getType());
		if (checker == null) {
			throw new QuestionStatusException(QuestionExceptionCode.UNAVAILABLE_TYPE);
		}
		return switch (question.getType()) {
			case MULTIPLE -> ((ReviewAnswerChecker<Long>)checker).check(questionId, question,
				(SubmitAnswerDto<Long>)submitAnswers);
			case SHORT -> ((ReviewAnswerChecker<String>)checker).check(questionId, question,
				(SubmitAnswerDto<String>)submitAnswers);
			case FILL_BLANK -> ((ReviewAnswerChecker<FillBlankSubmitAnswer>)checker).check(questionId,
				question, (SubmitAnswerDto<FillBlankSubmitAnswer>)submitAnswers);
			case ORDERING -> ((ReviewAnswerChecker<Long>)checker).check(questionId, question,
				(SubmitAnswerDto<Long>)submitAnswers);
		};
	}
}
