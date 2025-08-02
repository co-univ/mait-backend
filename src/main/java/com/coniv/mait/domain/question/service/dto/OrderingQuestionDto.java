package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;

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
		return this;
	}

	public static OrderingQuestionDto of(OrderingQuestionEntity orderingQuestion, List<OrderingOptionEntity> options) {
		List<OrderingQuestionOptionDto> optionDtos = options.stream()
			.map(OrderingQuestionOptionDto::from)
			.toList();

		return OrderingQuestionDto.builder()
			.id(orderingQuestion.getId())
			.content(orderingQuestion.getContent())
			.explanation(orderingQuestion.getExplanation())
			.number(orderingQuestion.getNumber())
			.options(optionDtos)
			.build();
	}
}
