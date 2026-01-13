package com.coniv.mait.domain.question.service.component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.question.service.dto.GradedAnswerShortResult;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.solve.util.AnswerProcessUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShortReviewAnswerChecker implements ReviewAnswerChecker<String> {

	private final ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.SHORT;
	}

	@Override
	public ReviewAnswerCheckResult check(Long questionId, QuestionEntity question,
		SubmitAnswerDto<String> submitAnswers) {
		List<String> submittedAnswers = submitAnswers.getSubmitAnswers();

		Map<Long, Set<String>> shortAnswersByNumber = shortAnswerEntityRepository
			.findAllByShortQuestionId(question.getId()).stream()
			.collect(Collectors.groupingBy(
				ShortAnswerEntity::getNumber,
				Collectors.mapping(
					shortAnswerEntity -> AnswerProcessUtil.processAnswer(shortAnswerEntity.getAnswer()),
					Collectors.toSet()
				)
			));

		if (submittedAnswers.size() != shortAnswersByNumber.size()) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.ANSWER_COUNT);
		}

		Map<Long, Boolean> shortAnswersByNumberChecked = shortAnswersByNumber.keySet().stream()
			.collect(Collectors.toMap(Function.identity(), value -> false));

		List<GradedAnswerShortResult> items = submittedAnswers.stream()
			.map(submitted -> {
				String processed = AnswerProcessUtil.processAnswer(submitted);
				boolean matchedAny = false;

				for (Long number : shortAnswersByNumber.keySet()) {
					if (shortAnswersByNumberChecked.get(number)) {
						// 이미 해당 그룹에 정답을 맞췄으면
						continue;
					}
					boolean contains = shortAnswersByNumber.get(number).contains(processed);
					shortAnswersByNumberChecked.put(number, contains);
					if (contains) {
						matchedAny = true;
					}
				}

				return new GradedAnswerShortResult(submitted, matchedAny);
			})
			.toList();

		boolean isCorrect = items.stream().allMatch(GradedAnswerShortResult::isCorrect);

		return ReviewAnswerCheckResult.builder()
			.questionId(questionId)
			.isCorrect(isCorrect)
			.type(QuestionType.SHORT)
			.items(items)
			.build();
	}
}
