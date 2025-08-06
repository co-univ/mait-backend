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

	private Integer answerCount;

	@Override
	public ShortQuestionDto toQuestionDto() {
		return ShortQuestionDto.builder()
			.id(getId())
			.content(getContent())
			.explanation(getExplanation())
			.number(getNumber())
			.shortAnswers(shortAnswers)
			.answerCount(answerCount)
			.build();
	}

	public static QuestionDto of(ShortQuestionEntity shortQuestion, List<ShortAnswerEntity> shortAnswers,
		boolean answerVisible) {
		List<ShortAnswerDto> shortAnswerDtos = answerVisible
			? shortAnswers.stream()
			.map(shortAnswerEntity -> ShortAnswerDto.of(shortAnswerEntity, answerVisible))
			.toList()
			: null;

		return ShortQuestionDto.builder()
			.id(shortQuestion.getId())
			.content(shortQuestion.getContent())
			.explanation(shortQuestion.getExplanation())
			.number(shortQuestion.getNumber())
			.shortAnswers(shortAnswerDtos)
			.answerCount(shortAnswers.size())
			.build();
	}
}

