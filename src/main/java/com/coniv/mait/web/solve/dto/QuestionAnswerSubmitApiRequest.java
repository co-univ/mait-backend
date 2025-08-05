package com.coniv.mait.web.solve.dto;

import com.coniv.mait.domain.question.constant.QuestionConstant;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

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
public abstract class QuestionAnswerSubmitApiRequest {

	@Schema(description = "문제 PK, 추후에 삭제 예정")
	@NotNull(message = "유저 ID를 입력해주세요.")
	private Long userId;

	public abstract SubmitAnswerDto<?> getSubmitAnswers();
}
