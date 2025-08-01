package com.coniv.mait.domain.question.service.dto;

import java.util.List;

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
}
