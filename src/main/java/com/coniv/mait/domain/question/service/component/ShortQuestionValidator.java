package com.coniv.mait.domain.question.service.component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.enums.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShortQuestionValidator implements QuestionValidator {

	private static final int MIN_ANSWER_COUNT = 1;

	private static final int MAX_ANSWER_COUNT = 5;

	private static final int MAX_SUB_ANSWER_COUNT = 5;

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

		long count = shortAnswers.stream().filter(ShortAnswerEntity::isMain).count();

		if (MIN_ANSWER_COUNT > count || count > MAX_ANSWER_COUNT) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.INVALID_ANSWER_COUNT);
		}

		if (shortAnswers.stream().anyMatch(answer -> answer.getAnswer().isBlank())) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.EMPTY_SHORT_ANSWER_CONTENT);
		}

		Map<Long, Long> numberByCount = shortAnswers.stream()
			.filter(shortAnswer -> !shortAnswer.isMain())
			.collect(Collectors.groupingBy(ShortAnswerEntity::getNumber, Collectors.counting()));

		if (numberByCount.values().stream().anyMatch(cnt -> cnt > MAX_SUB_ANSWER_COUNT)) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.INVALID_SUB_ANSWER_COUNT);
		}

		return QuestionValidateDto.valid(question);
	}
}
