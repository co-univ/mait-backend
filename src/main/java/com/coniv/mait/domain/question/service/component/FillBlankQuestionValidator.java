package com.coniv.mait.domain.question.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FillBlankQuestionValidator implements QuestionValidator {

	private final FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.FILL_BLANK;
	}

	@Override
	public QuestionValidateDto validate(QuestionEntity question) {
		if (!QuestionValidator.isQuestionValid(question)) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.EMPTY_CONTENT);
		}

		List<FillBlankAnswerEntity> fillBlankAnswers = fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(
			question.getId());

		if (fillBlankAnswers.stream().noneMatch(FillBlankAnswerEntity::isMain)) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.INVALID_BLANK_COUNT);
		}

		if (fillBlankAnswers.stream().anyMatch(answer -> answer.getAnswer().isBlank())) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.EMPTY_FILL_BLANK_ANSWER_CONTENT);
		}

		return QuestionValidateDto.valid(question);
	}
}
