package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrderingQuestionApiResponse extends QuestionApiResponse {

	@Schema(description = "순서 문제의 보기 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	private final List<OrderingOptionApiResponse> options;

	public static OrderingQuestionApiResponse from(OrderingQuestionDto dto) {
		List<OrderingOptionApiResponse> options = dto.getOptions().stream()
			.map(OrderingOptionApiResponse::from)
			.toList();

		return OrderingQuestionApiResponse.builder()
			.id(dto.getId())
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.number(dto.getNumber())
			.questionStatusType(dto.getQuestionStatus())
			.imageUrl(dto.getImageUrl())
			.imageId(dto.getImageId())
			.type(QuestionType.ORDERING)
			.options(options)
			.build();
	}
}
