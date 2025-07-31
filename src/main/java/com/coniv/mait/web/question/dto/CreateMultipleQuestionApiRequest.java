package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;

import jakarta.validation.Valid;
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
public class CreateMultipleQuestionApiRequest extends CreateQuestionApiRequest {

	@NotNull(message = "객관식 문제에는 반드시 선지가 있어야 합니다.")
	@Size(min = 2, max = 8, message = "객관식 문제는 최소 2개, 최대 8개의 선택지를 가져야 합니다.")
	@Valid
	private List<MultipleChoiceDto> choices;

	@Override
	public QuestionDto toQuestionDto() {
		return MultipleQuestionDto.builder()
			.content(getContent())
			.explanation(getExplanation())
			.number(getNumber())
			.choices(choices)
			.build();
	}
}
