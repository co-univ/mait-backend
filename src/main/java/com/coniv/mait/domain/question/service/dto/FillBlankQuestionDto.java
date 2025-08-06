package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;

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
public class FillBlankQuestionDto extends QuestionDto {

	private List<FillBlankAnswerDto> fillBlankAnswers;

	@Override
	public FillBlankQuestionDto toQuestionDto() {
		return this;
	}

	public static QuestionDto of(FillBlankQuestionEntity fillBlankQuestion,
		List<FillBlankAnswerEntity> fillBlankAnswers, boolean answerVisible) {
		List<FillBlankAnswerDto> fillBlankAnswerDtos = fillBlankAnswers.stream()
			.map(fillBlankAnswer -> FillBlankAnswerDto.of(fillBlankAnswer, answerVisible))
			.toList();

		return FillBlankQuestionDto.builder()
			.id(fillBlankQuestion.getId())
			.content(fillBlankQuestion.getContent())
			.explanation(fillBlankQuestion.getExplanation())
			.number(fillBlankQuestion.getNumber())
			.fillBlankAnswers(fillBlankAnswerDtos)
			.build();
	}
}
