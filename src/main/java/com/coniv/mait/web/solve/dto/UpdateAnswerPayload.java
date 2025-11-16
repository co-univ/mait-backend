package com.coniv.mait.web.solve.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수정할 문제 값", oneOf = {
	MultipleChoiceUpdateAnswerPayload.class,
	OrderingUpdateAnswerPayload.class,
	FillBlankUpdateAnswerPayload.class,
	ShortUpdateAnswerPayload.class
})
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = MultipleChoiceUpdateAnswerPayload.class, name = "MULTIPLE"),
	@JsonSubTypes.Type(value = ShortUpdateAnswerPayload.class, name = "SHORT"),
	@JsonSubTypes.Type(value = FillBlankUpdateAnswerPayload.class, name = "FILL_BLANK"),
	@JsonSubTypes.Type(value = OrderingUpdateAnswerPayload.class, name = "ORDERING")
})
public sealed interface UpdateAnswerPayload
	permits MultipleChoiceUpdateAnswerPayload, OrderingUpdateAnswerPayload, FillBlankUpdateAnswerPayload,
	ShortUpdateAnswerPayload {
}
