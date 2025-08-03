package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ShortQuestionDto extends QuestionDto {

	private List<ShortAnswerDto> shortAnswers;

	@Override
	public ShortQuestionDto toQuestionDto() {
		return ShortQuestionDto.builder()
			.id(getId())
			.content(getContent())
			.explanation(getExplanation())
			.number(getNumber())
			.shortAnswers(shortAnswers)
			.build();
	}

	public static QuestionDto of(ShortQuestionEntity shortQuestion, List<ShortAnswerEntity> shortAnswers) {
		List<ShortAnswerDto> shortAnswerDtos = shortAnswers.stream()
			.map(ShortAnswerDto::from)
			.toList();
		return ShortQuestionDto.builder()
			.id(shortQuestion.getId())
			.content(shortQuestion.getContent())
			.explanation(shortQuestion.getExplanation())
			.number(shortQuestion.getNumber())
			.shortAnswers(shortAnswerDtos)
			.build();
	}
}

