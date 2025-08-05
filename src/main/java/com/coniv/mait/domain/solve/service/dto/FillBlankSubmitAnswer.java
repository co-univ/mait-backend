package com.coniv.mait.domain.solve.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FillBlankSubmitAnswer(
	@NotNull(message = "빈칸 문제의 번호는 필수입니다.")
	Long number,
	@NotBlank(message = "빈칸 문제의 답변은 필수입니다.")
	String answer
) {

}
