package com.coniv.mait.domain.solve.service.component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MultipleQuestionAnswerChecker implements AnswerChecker<Long> {

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.MULTIPLE;
	}

	@Override
	public boolean checkAnswer(final QuestionEntity question, final SubmitAnswerDto<Long> request) {
		Set<Long> answerIds = multipleChoiceEntityRepository.findAllByQuestionId(question.getId()).stream()
			.map(MultipleChoiceEntity::getNumber)
			.map(Long::valueOf)
			.collect(Collectors.toSet());

		if (request.getType() != QuestionType.MULTIPLE) {
			throw new IllegalArgumentException("Invalid question type for MultipleQuestionAnswerChecker");
		}

		MultipleQuestionSubmitAnswer submitAnswer = (MultipleQuestionSubmitAnswer)request;

		Set<Long> submitAnswerIds = new HashSet<>(submitAnswer.getSelectedChoiceNumbers());

		return SetUtils.isEqualSet(answerIds, submitAnswerIds);
	}
}
