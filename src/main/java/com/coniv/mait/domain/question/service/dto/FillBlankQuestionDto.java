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

	private Integer blankCount;

	@Override
	public FillBlankQuestionDto toQuestionDto() {
		return FillBlankQuestionDto.builder()
			.id(getId())
			.content(getContent())
			.explanation(getExplanation())
			.number(getNumber())
			.fillBlankAnswers(fillBlankAnswers)
			.blankCount(blankCount)
			.build();
	}

	public static QuestionDto of(FillBlankQuestionEntity fillBlankQuestion,
		List<FillBlankAnswerEntity> fillBlankAnswers, boolean answerVisible) {
		List<FillBlankAnswerDto> fillBlankAnswerDtos = answerVisible
			? fillBlankAnswers.stream()
			.map(fillBlankAnswer -> FillBlankAnswerDto.of(fillBlankAnswer, answerVisible))
			.toList()
			: null;

		// 빈칸의 개수는 number별로 그룹화하여 계산 (각 빈칸마다 여러 정답이 있을 수 있음)
		int blankCount = (int)fillBlankAnswers.stream()
			.mapToLong(FillBlankAnswerEntity::getNumber)
			.distinct()
			.count();

		return FillBlankQuestionDto.builder()
			.id(fillBlankQuestion.getId())
			.content(fillBlankQuestion.getContent())
			.explanation(fillBlankQuestion.getExplanation())
			.number(fillBlankQuestion.getNumber())
			.fillBlankAnswers(fillBlankAnswerDtos)
			.blankCount(blankCount)
			.build();
	}
}
