package com.coniv.mait.domain.question.service.component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.GradedAnswerFillBlankResult;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.dto.FillBlankSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.solve.util.AnswerProcessUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FillBlankReviewAnswerChecker implements ReviewAnswerChecker<FillBlankSubmitAnswer> {

	private final FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.FILL_BLANK;
	}

	@Override
	public ReviewAnswerCheckResult check(Long questionId, QuestionEntity question,
		SubmitAnswerDto<FillBlankSubmitAnswer> submitAnswers) {
		List<FillBlankSubmitAnswer> blanks = submitAnswers.getSubmitAnswers();

		long distinctCount = blanks.stream().map(FillBlankSubmitAnswer::number).distinct().count();
		if (distinctCount != blanks.size()) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.DUPLICATE_ANSWER_NUMBER);
		}

		Map<Long, Set<String>> answersByNumber = fillBlankAnswerEntityRepository
			.findAllByFillBlankQuestionId(question.getId()).stream()
			.collect(Collectors.groupingBy(
				FillBlankAnswerEntity::getNumber,
				Collectors.mapping(
					entity -> AnswerProcessUtil.processAnswer(entity.getAnswer()),
					Collectors.toSet()
				)
			));

		List<GradedAnswerFillBlankResult> gradedResults = blanks.stream()
			.map(blank -> {
				Set<String> correctAnswers = answersByNumber.get(blank.number());
				if (correctAnswers == null || correctAnswers.isEmpty()) {
					return new GradedAnswerFillBlankResult(blank.number(), blank.answer(), false);
				}

				String processed = AnswerProcessUtil.processAnswer(blank.answer());
				boolean isCorrect = correctAnswers.contains(processed);
				return new GradedAnswerFillBlankResult(blank.number(), blank.answer(), isCorrect);
			})
			.toList();

		boolean isCorrect = gradedResults.stream().allMatch(GradedAnswerFillBlankResult::isCorrect);

		return ReviewAnswerCheckResult.builder()
			.questionId(questionId)
			.isCorrect(isCorrect)
			.type(QuestionType.FILL_BLANK)
			.gradedResults(gradedResults)
			.build();
	}
}
