package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMultipleQuestionApiRequest(
	String content,
	String explanation,
	Long number,
	@NotNull(message = "객관식 문제에는 반드시 선지가 있어야 합니다.")
	@Size(min = 2, max = 8, message = "객관식 문제는 최소 2개, 최대 8개의 선택지를 가져야 합니다.")
	List<MultipleChoiceDto> choices
) {

	public MultipleQuestionDto multipleQuestionDto() {
		return MultipleQuestionDto.builder()
			.content(content)
			.explanation(explanation)
			.number(number)
			.choices(choices)
			.build();
	}
}
