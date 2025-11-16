package com.coniv.mait.web.solve.dto;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.constraints.NotNull;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = MultipleChoiceAnswerPayload.class, name = "MULTIPLE"),
	@JsonSubTypes.Type(value = ShortAnswerPayload.class, name = "SHORT"),
	@JsonSubTypes.Type(value = FillBlankAnswerPayload.class, name = "FILL_BLANK"),
	@JsonSubTypes.Type(value = OrderingAnswerPayload.class, name = "ORDERING")
})
public sealed interface UpdateQuestionAnswerApiRequest
	permits MultipleChoiceAnswerPayload, OrderingAnswerPayload, FillBlankAnswerPayload, ShortAnswerPayload {

	@NotNull
	QuestionType type();
}
