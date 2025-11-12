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

	@Schema(description = "주관식 문제의 정답 목록", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private final List<ShortAnswerApiResponse> answers;

	@Schema(description = "주관식 문제의 정답 개수", requiredMode = Schema.RequiredMode.REQUIRED)
	private final Integer answerCount;

	public static ShortQuestionApiResponse from(ShortQuestionDto dto) {
		List<ShortAnswerApiResponse> answers = dto.getAnswers() != null
			? dto.getAnswers().stream()
			.map(ShortAnswerApiResponse::from)
			.toList()
			: null;

		return ShortQuestionApiResponse.builder()
			.id(dto.getId())
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.number(dto.getNumber())
			.questionStatusType(dto.getQuestionStatus())
			.imageUrl(dto.getImageUrl())
			.imageId(dto.getImageId())
			.type(QuestionType.SHORT)
			.answers(answers)
			.answerCount(dto.getAnswerCount())
			.build();
	}
}
