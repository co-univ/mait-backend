package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderingQuestionApiRequest extends CreateQuestionApiRequest {

	@Valid
	@NotNull(message = "선택지는 필수입니다.")
	private List<OrderingQuestionOptionDto> options;

	@Override
	public QuestionDto toQuestionDto() {
		return OrderingQuestionDto.builder()
			.content(getContent())
			.explanation(getExplanation())
			.number(getNumber())
			.options(getOptions())
			.build();
	}
}
