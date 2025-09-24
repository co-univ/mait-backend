package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.constant.QuestionConstant;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type",
	include = JsonTypeInfo.As.EXISTING_PROPERTY
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

	@NotNull(message = "문제 번호는 필수입니다.")
	@Min(value = 1, message = "문제 번호는 1 이상이어야 합니다.")
	private Long number;

	@NotNull(message = "문제 유형은 필수입니다.")
	private String type;

	public abstract QuestionDto toQuestionDto();

	protected void validateType(String expectedType) {
		if (!expectedType.equals(this.type)) {
			throw new IllegalArgumentException(
				String.format("문제 유형이 일치하지 않습니다. 예상: %s, 실제: %s", expectedType, this.type)
			);
		}
	}
}
