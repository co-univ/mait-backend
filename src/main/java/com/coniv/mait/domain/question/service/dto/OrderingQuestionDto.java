package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;

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
	public QuestionType getType() {
		return QuestionType.ORDERING;
	}

	@Override
	public OrderingQuestionDto toQuestionDto() {
		return this;
	}

	public static OrderingQuestionDto of(OrderingQuestionEntity orderingQuestion, List<OrderingOptionEntity> options,
		boolean answerVisible) {
		List<OrderingQuestionOptionDto> optionDtos = options.stream()
			.map(option -> OrderingQuestionOptionDto.of(option, answerVisible))
			.toList();

		return OrderingQuestionDto.builder()
			.id(orderingQuestion.getId())
			.content(orderingQuestion.getContent())
			.explanation(orderingQuestion.getExplanation())
			.number(orderingQuestion.getNumber())
			.questionStatus(orderingQuestion.getQuestionStatus())
			.imageUrl(orderingQuestion.getImageUrl())
			.options(optionDtos)
			.build();
	}
}
