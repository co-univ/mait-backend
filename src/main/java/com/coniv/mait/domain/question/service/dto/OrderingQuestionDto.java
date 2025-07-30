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
public class OrderingQuestionDto extends QuestionDto {

	private List<OrderingQuestionOptionDto> options;

	@Override
	public OrderingQuestionDto toQuestionDto() {
		return null;
	}
}
