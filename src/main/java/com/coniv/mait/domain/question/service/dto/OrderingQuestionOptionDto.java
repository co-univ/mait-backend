package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderingQuestionOptionDto {

	private Long id;

	@Schema(description = "보기에 보여질 순서", examples = {"A", "B", "C"}, requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "보기에 보여질 순서는 필수입니다.")
	private int originOrder;

	@Schema(description = "보기 내용", examples = {"보기 내용 1", "보기 내용 2"}, requiredMode = Schema.RequiredMode.REQUIRED)
	private String content;

	@Schema(description = "정답이 되는 순서", examples = {"1", "2", "3"}, requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "정답이 되는 순서는 필수입니다.")
	private int answerOrder;

	public static OrderingQuestionOptionDto from(OrderingOptionEntity orderingOptionEntity) {
		return OrderingQuestionOptionDto.builder()
			.id(orderingOptionEntity.getId())
			.originOrder(orderingOptionEntity.getOriginOrder())
			.content(orderingOptionEntity.getContent())
			.answerOrder(orderingOptionEntity.getAnswerOrder())
			.build();
	}
}
