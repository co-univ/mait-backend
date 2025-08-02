package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ShortQuestionApiResponse extends QuestionApiResponse {

	@Schema(description = "주관식 문제의 정답 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	private final List<ShortAnswerApiResponse> answers;

	public static ShortQuestionApiResponse from(ShortQuestionDto dto) {
		List<ShortAnswerApiResponse> answers = dto.getShortAnswers().stream()
			.map(ShortAnswerApiResponse::from)
			.toList();

		return ShortQuestionApiResponse.builder()
			.id(dto.getId())
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.number(dto.getNumber())
			.type(QuestionType.SHORT)
			.answers(answers)
			.build();
	}
}
