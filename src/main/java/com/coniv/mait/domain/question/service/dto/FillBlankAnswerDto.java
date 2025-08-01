package com.coniv.mait.domain.question.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FillBlankAnswerDto {

	private Long id;

	private String answer;

	@Schema(description = "빈칸 문제의 메인 정답 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	private boolean isMain;

	@Schema(description = "빈칸 문제 정답 그룹 번호", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "빈칸 문제 정답 그룹 번호는 필수입니다.")
	private Long number;
}
