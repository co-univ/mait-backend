package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MultipleQuestionApiResponse extends QuestionApiResponse {

	@Schema(description = "객관식 문제의 선택지 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	private final List<MultipleChoiceApiResponse> choices;

	public static MultipleQuestionApiResponse from(MultipleQuestionDto dto) {
		List<MultipleChoiceApiResponse> choices = dto.getChoices().stream()
			.map(MultipleChoiceApiResponse::from)
			.toList();

		return MultipleQuestionApiResponse.builder()
			.id(dto.getId())
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.number(dto.getNumber())
			.questionStatusType(dto.getQuestionStatus())
			.imageUrl(dto.getImageUrl())
			.type(QuestionType.MULTIPLE)
			.choices(choices)
			.build();
	}
}
