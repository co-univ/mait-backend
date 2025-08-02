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

	@Schema(description = "빈칸 문제의 정답 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	private final List<FillBlankAnswerApiResponse> answers;

	public static FillBlankQuestionApiResponse from(FillBlankQuestionDto dto) {
		List<FillBlankAnswerApiResponse> answers = dto.getFillBlankAnswers().stream()
			.map(FillBlankAnswerApiResponse::from)
			.toList();

		return FillBlankQuestionApiResponse.builder()
			.id(dto.getId())
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.number(dto.getNumber())
			.questionType(QuestionType.FILL_BLANK)
			.answers(answers)
			.build();
	}
}
