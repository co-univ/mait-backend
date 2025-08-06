package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class FillBlankQuestionApiResponse extends QuestionApiResponse {

	@Schema(description = "빈칸 문제의 정답 목록", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private final List<FillBlankAnswerApiResponse> answers;

	@Schema(description = "빈칸의 개수", requiredMode = Schema.RequiredMode.REQUIRED)
	private final Integer blankCount;

	public static FillBlankQuestionApiResponse from(FillBlankQuestionDto dto) {
		List<FillBlankAnswerApiResponse> answers = dto.getFillBlankAnswers() != null
			? dto.getFillBlankAnswers().stream()
			.map(FillBlankAnswerApiResponse::from)
			.toList()
			: null;

		return FillBlankQuestionApiResponse.builder()
			.id(dto.getId())
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.number(dto.getNumber())
			.type(QuestionType.FILL_BLANK)
			.answers(answers)
			.blankCount(dto.getBlankCount())
			.build();
	}
}
