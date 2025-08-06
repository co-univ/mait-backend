package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MultipleChoiceDto {

	private Long id;

	@NotNull(message = "객관식 선지의 번호는 필수입니다.")
	@Min(value = 1, message = "객관식 선지의 번호는 1 이상이어야 합니다.")
	private int number;

	@NotBlank(message = "객관식 선지의 내용은 필수입니다.")
	private String content;

	@NotNull(message = "객관식 선지의 정답 여부는 필수입니다.")
	private Boolean isCorrect;

	public static MultipleChoiceDto of(final MultipleChoiceEntity multipleChoice, final boolean answerVisible) {
		return MultipleChoiceDto.builder()
			.id(multipleChoice.getId())
			.number(multipleChoice.getNumber())
			.content(multipleChoice.getContent())
			.isCorrect(answerVisible ? multipleChoice.isCorrect() : null)
			.build();
	}
}
