package com.coniv.mait.domain.question.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShortQuestionValidator implements QuestionValidator {

	private final ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.SHORT;
	}

	@Override
	public QuestionValidateDto validate(QuestionEntity question) {
		if (!QuestionValidator.isQuestionValid(question)) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.EMPTY_CONTENT);
		}

		List<ShortAnswerEntity> shortAnswers = shortAnswerEntityRepository.findAllByShortQuestionId(
			question.getId());

		if (shortAnswers.stream().noneMatch(ShortAnswerEntity::isMain)) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.INVALID_ANSWER_COUNT);
		}

		if (shortAnswers.stream().anyMatch(answer -> answer.getAnswer().isBlank())) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.EMPTY_SHORT_ANSWER_CONTENT);
		}

		return QuestionValidateDto.valid(question);
	}
}
