package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.enums.QuestionType;

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
