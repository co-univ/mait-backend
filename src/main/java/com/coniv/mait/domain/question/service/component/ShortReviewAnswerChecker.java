package com.coniv.mait.domain.question.service.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.GradedAnswerShortResult;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
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

		List<String> processedAnswers = submittedAnswers.stream()
			.map(AnswerProcessUtil::processAnswer)
			.toList();
		Map<Long, Integer> matchedAnswerIndexesByNumber = new HashMap<>();
		List<GradedAnswerShortResult> gradedResults = new ArrayList<>();
		for (int answerIndex = 0; answerIndex < submittedAnswers.size(); answerIndex++) {
			boolean isCorrect = matchAnswerGroup(processedAnswers, answerIndex, shortAnswersByNumber,
				matchedAnswerIndexesByNumber, new HashSet<>());
			gradedResults.add(new GradedAnswerShortResult(submittedAnswers.get(answerIndex), isCorrect));
		}

		boolean isCorrect = gradedResults.stream().allMatch(GradedAnswerShortResult::isCorrect);

		return ReviewAnswerCheckResult.builder()
			.questionId(questionId)
			.isCorrect(isCorrect)
			.type(QuestionType.SHORT)
			.gradedResults(gradedResults)
			.build();
	}

	private boolean matchAnswerGroup(List<String> processedAnswers, int answerIndex,
		Map<Long, Set<String>> shortAnswersByNumber, Map<Long, Integer> matchedAnswerIndexesByNumber,
		Set<Long> visitedNumbers) {
		String submittedAnswer = processedAnswers.get(answerIndex);
		for (Map.Entry<Long, Set<String>> answerGroup : shortAnswersByNumber.entrySet()) {
			Long number = answerGroup.getKey();
			if (!answerGroup.getValue().contains(submittedAnswer) || !visitedNumbers.add(number)) {
				continue;
			}

			Integer matchedAnswerIndex = matchedAnswerIndexesByNumber.get(number);
			// 기존 답을 다른 그룹으로 옮길 수 있을 때만 현재 답에 이 그룹을 배정한다.
			if (matchedAnswerIndex != null && !matchAnswerGroup(processedAnswers, matchedAnswerIndex,
				shortAnswersByNumber, matchedAnswerIndexesByNumber, visitedNumbers)) {
				continue;
			}

			matchedAnswerIndexesByNumber.put(number, answerIndex);
			return true;
		}

		return false;
	}
}
