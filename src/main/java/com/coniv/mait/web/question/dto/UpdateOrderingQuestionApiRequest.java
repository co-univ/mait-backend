package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateOrderingQuestionApiRequest extends UpdateQuestionApiRequest {

	@Valid
	@NotNull(message = "선택지는 필수입니다.")
	private List<OrderingQuestionOptionDto> options;

	@Override
	public QuestionDto toQuestionDto() {
		return OrderingQuestionDto.builder()
			.id(getId())
			.content(getContent())
			.explanation(getExplanation())
			.number(getNumber())
			.options(options)
			.build();
	}
}
