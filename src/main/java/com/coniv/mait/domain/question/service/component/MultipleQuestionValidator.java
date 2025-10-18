package com.coniv.mait.domain.question.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MultipleQuestionValidator implements QuestionValidator {

	private static final int MIN_CHOICES = 2;
	private static final int MAX_CHOICES = 8;

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.MULTIPLE;
	}

	@Override
	public QuestionValidateDto validate(QuestionEntity question) {
		if (!QuestionValidator.isQuestionValid(question)) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.EMPTY_CONTENT);
		}

		MultipleQuestionEntity multipleQuestion = (MultipleQuestionEntity)question;

		List<MultipleChoiceEntity> choices = multipleChoiceEntityRepository.findAllByQuestionId(
			multipleQuestion.getId());

		if (choices.size() < MIN_CHOICES || choices.size() > MAX_CHOICES) {
			return QuestionValidateDto.invalid(multipleQuestion, QuestionValidationResult.INVALID_CHOICE_COUNT);
		}

		if (choices.stream().noneMatch(MultipleChoiceEntity::isCorrect)) {
			return QuestionValidateDto.invalid(multipleQuestion, QuestionValidationResult.NO_CORRECT_CHOICE);
		}

		if (choices.stream().anyMatch(choice -> choice.getContent().isBlank())) {
			return QuestionValidateDto.invalid(multipleQuestion, QuestionValidationResult.EMPTY_CHOICE_CONTENT);
		}

		return QuestionValidateDto.valid(multipleQuestion);
	}
}
