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

	@Schema(description = "업로드하려는 이미지의 url")
	private String imageUrl;

	@Schema(description = "업로드하려는 이미지의 PK")
	private Long imageId;

	public abstract QuestionDto toQuestionDto();
}
