package com.coniv.mait.domain.question.service.component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

@Component
public class ShortQuestionFactory {

	private static final int MAX_DISPLAY_DELAY_MILLISECONDS = 5000;

	public ShortQuestionEntity create(ShortQuestionDto dto, QuestionSetEntity questionSetEntity) {
		return ShortQuestionEntity.builder()
			.number(dto.getNumber())
			.questionSet(questionSetEntity)
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.displayDelayMilliseconds(RandomUtil.getRandomNumber(MAX_DISPLAY_DELAY_MILLISECONDS))
			.answerCount(dto.getShortAnswers().size())
			.build();
	}

	public List<ShortAnswerEntity> createShortAnswers(List<ShortAnswerDto> shortAnswerDtos,
		ShortQuestionEntity shortQuestionEntity) {
		checkDuplicateNumber(shortAnswerDtos);
		return shortAnswerDtos.stream()
			.map(shortAnswerDto -> ShortAnswerEntity.builder()
				.answer(shortAnswerDto.getAnswer())
				.isMain(shortAnswerDto.isMain())
				.number(shortAnswerDto.getNumber())
				.shortAnswerId(shortQuestionEntity.getId())
				.build())
			.toList();
	}

	private void checkDuplicateNumber(List<ShortAnswerDto> shortAnswerDtos) {
		Set<Long> numbers = shortAnswerDtos.stream().map(ShortAnswerDto::getNumber)
			.collect(Collectors.toSet());
		if (numbers.size() != shortAnswerDtos.size()) {
			throw new UserParameterException("Short answer numbers must be unique.");
		}
	}
}
