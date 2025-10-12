package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = MultipleQuestionApiResponse.class, name = "MULTIPLE"),
	@JsonSubTypes.Type(value = ShortQuestionApiResponse.class, name = "SHORT"),
	@JsonSubTypes.Type(value = OrderingQuestionApiResponse.class, name = "ORDERING"),
	@JsonSubTypes.Type(value = FillBlankQuestionApiResponse.class, name = "FILL_BLANK")
})
@SuperBuilder
public abstract class QuestionApiResponse {

	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	private Long id;

	private String content;

	private String explanation;

	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private Long number;

	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	private QuestionType type;

	private QuestionStatusType questionStatusType;

	public static QuestionApiResponse from(QuestionDto questionDto) {
		return switch (questionDto) {
			case MultipleQuestionDto multiple -> MultipleQuestionApiResponse.from(multiple);
			case ShortQuestionDto shortQuestion -> ShortQuestionApiResponse.from(shortQuestion);
			case OrderingQuestionDto ordering -> OrderingQuestionApiResponse.from(ordering);
			case FillBlankQuestionDto fillBlank -> FillBlankQuestionApiResponse.from(fillBlank);
			default -> throw new IllegalArgumentException("Unknown question type: " + questionDto.getClass());
		};
	}
}
