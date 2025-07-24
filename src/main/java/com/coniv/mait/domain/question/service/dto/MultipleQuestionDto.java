package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MultipleQuestionDto {

	private Long id;

	private String content;

	private String explanation;

	@NotNull(message = "문제의 번호는 필수입니다.")
	@Min(value = 1, message = "문제의 번호는 1 이상이어야 합니다.")
	private Long number;

	@NotNull
	@Size(min = 2, max = 8, message = "객관식 선지의 개수는 2 ~ 8개여야 합니다.")
	private List<MultipleChoiceDto> choices;
}
