package com.coniv.mait.domain.question.service.component;

import java.util.List;
import java.util.Set;
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
		checkDuplicateNumber(fillBlankAnswerDtos);
		return fillBlankAnswerDtos.stream()
			.map(fillBlankAnswerDto -> FillBlankAnswerEntity.builder()
				.answer(fillBlankAnswerDto.getAnswer())
				.isMain(fillBlankAnswerDto.isMain())
				.number(fillBlankAnswerDto.getNumber())
				.fillBlankQuestionId(fillBlankQuestionEntity.getId())
				.build())
			.toList();
	}

	private void checkDuplicateNumber(List<FillBlankAnswerDto> fillBlankAnswerDtos) {
		Set<Long> numbers = fillBlankAnswerDtos.stream()
			.map(FillBlankAnswerDto::getNumber)
			.collect(Collectors.toSet());
		if (numbers.size() != fillBlankAnswerDtos.size()) {
			throw new UserParameterException("Fill blank answer numbers must be unique.");
		}
	}
}
