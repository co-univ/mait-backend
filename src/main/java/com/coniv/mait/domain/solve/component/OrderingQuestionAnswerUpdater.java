package com.coniv.mait.domain.solve.component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.web.solve.dto.OptionOrderPatch;
import com.coniv.mait.web.solve.dto.OrderingUpdateAnswerPayload;
import com.coniv.mait.web.solve.dto.UpdateAnswerPayload;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderingQuestionAnswerUpdater implements QuestionAnswerUpdater {

	private final OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.ORDERING;
	}

	@Override
	@Transactional
	public void updateAnswer(QuestionEntity question, UpdateAnswerPayload payload) {
		if (!(payload instanceof OrderingUpdateAnswerPayload orderingPayload)) {
			throw new IllegalArgumentException("순서형 문제에만 OrderingQuestionAnswerUpdater를 사용할 수 있습니다.");
		}

		Map<Long, OrderingOptionEntity> optionById = orderingOptionEntityRepository.findAllByOrderingQuestionId(
				question.getId()).stream()
			.collect(Collectors.toUnmodifiableMap(OrderingOptionEntity::getId, Function.identity()));

		if (optionById.keySet().size() != orderingPayload.options().size()) {
			throw new IllegalArgumentException("제출된 옵션의 개수가 문제의 옵션 개수와 일치하지 않습니다.");
		}

		for (OptionOrderPatch option : orderingPayload.options()) {
			OrderingOptionEntity existingOption = optionById.get(option.optionId());
			if (existingOption == null) {
				throw new EntityNotFoundException("존재하지 않는 옵션 ID가 포함되어 있습니다: " + option.optionId());
			}
			existingOption.updateAnswerOrder(option.answerOrder());
		}
	}
}
