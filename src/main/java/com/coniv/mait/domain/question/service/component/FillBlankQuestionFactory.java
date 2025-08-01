package com.coniv.mait.domain.question.service.component;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

@Component
public class FillBlankQuestionFactory {

	private static final int MAX_DISPLAY_DELAY_MILLISECONDS = 5000;

	public FillBlankQuestionEntity create(FillBlankQuestionDto dto, QuestionSetEntity questionSetEntity) {
		return FillBlankQuestionEntity.builder()
			.number(dto.getNumber())
			.questionSet(questionSetEntity)
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.displayDelayMilliseconds(RandomUtil.getRandomNumber(MAX_DISPLAY_DELAY_MILLISECONDS))
			.build();
	}

	public List<FillBlankAnswerEntity> createFillBlankAnswers(
		List<FillBlankAnswerDto> fillBlankAnswerDtos, FillBlankQuestionEntity fillBlankQuestionEntity) {
		validateMainAnswers(fillBlankAnswerDtos);
		return fillBlankAnswerDtos.stream()
			.map(dto -> createFillBlankAnswerEntity(dto, fillBlankQuestionEntity.getId()))
			.toList();
	}

	private FillBlankAnswerEntity createFillBlankAnswerEntity(FillBlankAnswerDto dto, Long questionId) {
		return FillBlankAnswerEntity.builder()
			.answer(dto.getAnswer())
			.isMain(dto.isMain())
			.number(dto.getNumber())
			.fillBlankQuestionId(questionId)
			.build();
	}

	private void validateMainAnswers(List<FillBlankAnswerDto> answers) {
		answers.stream()
			.collect(Collectors.groupingBy(FillBlankAnswerDto::getNumber))
			.forEach(this::validateSingleMainAnswer);
	}

	private void validateSingleMainAnswer(Long number, List<FillBlankAnswerDto> answersForNumber) {
		long mainCount = answersForNumber.stream()
			.mapToLong(answer -> answer.isMain() ? 1 : 0)
			.sum();

		if (mainCount != 1) {
			throw new UserParameterException(
				String.format("Each blank number must have exactly one main answer. Number %d has %d main answers.",
					number, mainCount));
		}
	}
}
