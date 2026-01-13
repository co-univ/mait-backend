package com.coniv.mait.domain.question.service.component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.question.service.dto.GradedAnswerMultipleResult;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MultipleReviewAnswerChecker implements ReviewAnswerChecker<Long> {

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.MULTIPLE;
	}

	@Override
	public ReviewAnswerCheckResult check(Long questionId, QuestionEntity question,
		SubmitAnswerDto<Long> submitAnswers) {
		Set<Long> correctNumbers = multipleChoiceEntityRepository.findAllByQuestionId(question.getId()).stream()
			.filter(MultipleChoiceEntity::isCorrect)
			.map(MultipleChoiceEntity::getNumber)
			.map(Long::valueOf)
			.collect(Collectors.toSet());

		MultipleQuestionSubmitAnswer multiple = (MultipleQuestionSubmitAnswer)submitAnswers;
		List<GradedAnswerMultipleResult> gradedResults = multiple.getSubmitAnswers().stream()
			.map(submittedNumber -> new GradedAnswerMultipleResult(submittedNumber,
				correctNumbers.contains(submittedNumber)))
			.toList();

		// 객관식은 "100% 일치"가 정답 조건이므로, all-true 뿐 아니라 정답 개수도 만족해야 한다.
		Set<Long> submittedSet = new HashSet<>(multiple.getSubmitAnswers());
		boolean isCorrect = submittedSet.equals(correctNumbers);

		return ReviewAnswerCheckResult.builder()
			.questionId(questionId)
			.isCorrect(isCorrect)
			.type(QuestionType.MULTIPLE)
			.gradedResults(gradedResults)
			.build();
	}
}
