package com.coniv.mait.domain.question.service.component;

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
		// 순서유형은 "실시간과 동일" 정책이라 오답이어도 추가 피드백을 내리지 않는다.
		return ReviewAnswerCheckResult.builder()
			.questionId(questionId)
			.isCorrect(answerGrader.gradeAnswer(question, submitAnswers))
			.type(QuestionType.ORDERING)
			.items(null)
			.build();
	}
}
