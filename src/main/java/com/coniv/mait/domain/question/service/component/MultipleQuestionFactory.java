package com.coniv.mait.domain.question.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MultipleQuestionFactory {

	private static final int MAX_DISPLAY_DELAY_MILLISECONDS = 5000;

	public MultipleQuestionEntity create(MultipleQuestionDto dto, QuestionSetEntity questionSet) {
		return MultipleQuestionEntity.builder()
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.number(dto.getNumber())
			.displayDelayMilliseconds(RandomUtil.getRandomNumber(MAX_DISPLAY_DELAY_MILLISECONDS))
			.questionSet(questionSet)
			.answerCount(calculateAnswerCount(dto.getChoices()))
			.build();
	}

	public List<MultipleChoiceEntity> createChoices(
		List<MultipleChoiceDto> dtos,
		MultipleQuestionEntity question
	) {
		checkChoicesNumber(dtos);
		return dtos.stream()
			.map(dto -> createChoice(dto, question))
			.toList();
	}

	private void checkChoicesNumber(List<MultipleChoiceDto> dtos) {
		long count = dtos.stream()
			.map(MultipleChoiceDto::getNumber)
			.distinct()
			.count();

		if (count != dtos.size()) {
			throw new UserParameterException("중복된 선택지 번호가 존재합니다.");
		}
	}

	private MultipleChoiceEntity createChoice(MultipleChoiceDto dto, MultipleQuestionEntity question) {
		return MultipleChoiceEntity.builder()
			.number(dto.getNumber())
			.content(dto.getContent())
			.question(question)
			.isCorrect(dto.getIsCorrect())
			.build();
	}

	private int calculateAnswerCount(List<MultipleChoiceDto> choices) {
		return (int)choices.stream().filter(choice -> choice.getIsCorrect()).count();
	}
}
