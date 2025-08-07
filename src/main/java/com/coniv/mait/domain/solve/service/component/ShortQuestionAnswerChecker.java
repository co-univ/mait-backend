package com.coniv.mait.domain.solve.service.component;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
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

		if (answers.getSubmitAnswers().size() != shortAnswersByNumber.keySet().size()) {
			throw new IllegalArgumentException("제출된 답변의 개수와 문제의 답변 개수가 일치하지 않습니다.");
		}

		Map<Long, Boolean> shortAnswersByNumberChecked = shortAnswersByNumber.keySet().stream()
			.collect(Collectors.toMap(Function.identity(), value -> false));

		for (String submitAnswer : answers.getSubmitAnswers()) {
			String processedSubmitAnswer = AnswerProcessUtil.processAnswer(submitAnswer);
			for (Long number : shortAnswersByNumber.keySet()) {
				if (shortAnswersByNumberChecked.get(number)) {
					continue;
				}
				boolean contains = shortAnswersByNumber.get(number).contains(processedSubmitAnswer);
				shortAnswersByNumberChecked.put(number, contains);
			}
		}

		return shortAnswersByNumberChecked.values().stream().allMatch(a -> a);
	}
}
