package com.coniv.mait.web.solve.dto;

import com.coniv.mait.domain.question.constant.QuestionConstant;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = ShortQuestionSubmitApiRequest.class, name = QuestionConstant.SHORT),
	@JsonSubTypes.Type(value = MultipleQuestionSubmitApiRequest.class, name = QuestionConstant.MULTIPLE),
	@JsonSubTypes.Type(value = OrderingQuestionSubmitApiRequest.class, name = QuestionConstant.ORDERING),
	@JsonSubTypes.Type(value = FillBlankQuestionSubmitApiRequest.class, name = QuestionConstant.FILL_BLANK)
})
@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class QuestionAnswerSubmitApiRequest {

	public abstract SubmitAnswerDto<?> getSubmitAnswers();
}
