package com.coniv.mait.domain.solve.service.component;

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
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.solve.util.AnswerProcessUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShortQuestionAnswerChecker implements AnswerChecker<String> {

	private final ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.SHORT;
	}

	@Override
	public boolean checkAnswer(QuestionEntity question, SubmitAnswerDto<String> answers) {
		Map<Long, Set<String>> shortAnswersByNumber = shortAnswerEntityRepository
			.findAllByShortQuestionId(question.getId()).stream()
			.collect(Collectors.groupingBy(
				ShortAnswerEntity::getNumber,
				Collectors.mapping(
					shortAnswerEntity -> AnswerProcessUtil.processAnswer(shortAnswerEntity.getAnswer()),
					Collectors.toSet()
				)
			));

		if (answers.getSubmitAnswers().size() != shortAnswersByNumber.size()) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.ANSWER_COUNT);
		}

		List<String> submitAnswers = answers.getSubmitAnswers().stream()
			.map(AnswerProcessUtil::processAnswer)
			.toList();

		return matchAnswerGroups(submitAnswers, 0, shortAnswersByNumber, new HashSet<>());
	}

	private boolean matchAnswerGroups(List<String> submitAnswers, int answerIndex,
		Map<Long, Set<String>> shortAnswersByNumber, Set<Long> matchedNumbers) {
		if (answerIndex == submitAnswers.size()) {
			return true;
		}

		String submitAnswer = submitAnswers.get(answerIndex);
		for (Map.Entry<Long, Set<String>> answerGroup : shortAnswersByNumber.entrySet()) {
			Long number = answerGroup.getKey();
			if (matchedNumbers.contains(number) || !answerGroup.getValue().contains(submitAnswer)) {
				continue;
			}

			matchedNumbers.add(number);
			if (matchAnswerGroups(submitAnswers, answerIndex + 1, shortAnswersByNumber, matchedNumbers)) {
				return true;
			}
			matchedNumbers.remove(number);
		}

		return false;
	}
}
