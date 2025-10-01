package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.constant.QuestionConstant;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type",
	visible = true
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = UpdateShortQuestionApiRequest.class, name = QuestionConstant.SHORT),
	@JsonSubTypes.Type(value = UpdateMultipleQuestionApiRequest.class, name = QuestionConstant.MULTIPLE),
	@JsonSubTypes.Type(value = UpdateOrderingQuestionApiRequest.class, name = QuestionConstant.ORDERING),
	@JsonSubTypes.Type(value = UpdateFillBlankQuestionApiRequest.class, name = QuestionConstant.FILL_BLANK)
})
@Data
public abstract class UpdateQuestionApiRequest {

	private Long id;

	private String content;

	private String explanation;

	@Schema(description = "문제 번호, 단 문제 번호는 변경되지 않음")
	@NotNull(message = "문제 번호는 필수입니다.")
	@Min(value = 1, message = "문제 번호는 1 이상이어야 합니다.")
	private Long number;

	public abstract QuestionDto toQuestionDto();
}
