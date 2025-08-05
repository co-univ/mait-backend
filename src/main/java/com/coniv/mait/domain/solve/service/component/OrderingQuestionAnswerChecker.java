package com.coniv.mait.domain.solve.service.component;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderingQuestionAnswerChecker implements AnswerChecker<Long> {

	private final OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.ORDERING;
	}

	@Override
	public boolean checkAnswer(QuestionEntity question, SubmitAnswerDto<Long> answers) {
		List<Long> answerOrders = orderingOptionEntityRepository.findAllByOrderingQuestionId(question.getId()).stream()
			.sorted(Comparator.comparing(OrderingOptionEntity::getAnswerOrder))
			.map(OrderingOptionEntity::getOriginOrder)
			.map(Long::valueOf)
			.toList();

		List<Long> submitAnswers = answers.getSubmitAnswers();

		return answerOrders.equals(submitAnswers);
	}
}
