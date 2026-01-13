package com.coniv.mait.domain.question.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderingReviewAnswerChecker implements ReviewAnswerChecker<Long> {

	private final AnswerGrader answerGrader;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.ORDERING;
	}

	@Override
	public ReviewAnswerCheckResult check(Long questionId, QuestionEntity question,
		SubmitAnswerDto<Long> submitAnswers) {
		return ReviewAnswerCheckResult.builder()
			.questionId(questionId)
			.isCorrect(answerGrader.gradeAnswer(question, submitAnswers))
			.type(QuestionType.ORDERING)
			.gradedResults(List.of())
			.build();
	}
}
