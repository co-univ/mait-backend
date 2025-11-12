package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "questionType",
	visible = true
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = MultipleQuestionDto.class, name = "MULTIPLE"),
	@JsonSubTypes.Type(value = ShortQuestionDto.class, name = "SHORT"),
	@JsonSubTypes.Type(value = FillBlankQuestionDto.class, name = "FILL_BLANK"),
	@JsonSubTypes.Type(value = OrderingQuestionDto.class, name = "ORDERING")
})
public abstract class QuestionDto {

	private Long id;

	private String content;

	private String explanation;

	private String imageUrl;

	private Long imageId;

	@NotNull(message = "문제의 번호는 필수입니다.")
	@Min(value = 1, message = "문제의 번호는 1 이상이어야 합니다.")
	private Long number;

	private QuestionStatusType questionStatus;

	public abstract QuestionDto toQuestionDto();

	public abstract QuestionType getType();
}
