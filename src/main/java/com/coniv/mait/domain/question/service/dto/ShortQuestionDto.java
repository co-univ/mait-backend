package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonProperty;

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

	@JsonProperty("answers")
	private List<ShortAnswerDto> answers;

	private Integer answerCount;

	@Override
	public QuestionType getType() {
		return QuestionType.SHORT;
	}

	@Override
	public ShortQuestionDto toQuestionDto() {
		return this;
	}

	public static ShortQuestionDto of(ShortQuestionEntity shortQuestion, List<ShortAnswerEntity> shortAnswers,
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
			.imageUrl(shortQuestion.getImageUrl())
			.imageId(shortQuestion.getImageId())
			.answers(shortAnswerDtos)
			.questionStatus(shortQuestion.getQuestionStatus())
			.answerCount(shortAnswers.size())
			.build();
	}
}

