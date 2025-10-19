package com.coniv.mait.domain.question.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderingQuestionValidator implements QuestionValidator {

	private static final int MIN_OPTIONS = 2;

	private final OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.ORDERING;
	}

	@Override
	public QuestionValidateDto validate(QuestionEntity question) {
		if (!QuestionValidator.isQuestionValid(question)) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.EMPTY_CONTENT);
		}

		List<OrderingOptionEntity> options = orderingOptionEntityRepository.findAllByOrderingQuestionId(
			question.getId());

		if (options.size() < MIN_OPTIONS) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.INVALID_OPTION_COUNT);
		}

		if (options.stream().anyMatch(option -> option.getContent().isBlank())) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.EMPTY_ORDERING_OPTION_CONTENT);
		}

		return QuestionValidateDto.valid(question);
	}
}
