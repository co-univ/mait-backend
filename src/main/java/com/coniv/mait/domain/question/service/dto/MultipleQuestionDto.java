package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class MultipleQuestionDto extends QuestionDto {

	@NotNull
	@Size(min = 2, max = 8, message = "객관식 선지의 개수는 2 ~ 8개여야 합니다.")
	private List<MultipleChoiceDto> choices;

	@Override
	public QuestionDto toQuestionDto() {
		return this;
	}
}
