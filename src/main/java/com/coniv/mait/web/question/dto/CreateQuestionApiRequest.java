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
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = CreateShortQuestionApiRequest.class, name = QuestionContstant.SHORT),
	@JsonSubTypes.Type(value = CreateMultipleQuestionApiRequest.class, name = QuestionContstant.MULTIPLE)
})
@Data
public abstract class CreateQuestionApiRequest {

	private String content;

	private String explanation;

	@NotNull(message = "문제 번호는 필수입니다.")
	@Min(value = 1, message = "문제 번호는 1 이상이어야 합니다.")
	private Long number;

	// 각 하위 클래스에서 구현
	public abstract QuestionDto toQuestionDto();
}
