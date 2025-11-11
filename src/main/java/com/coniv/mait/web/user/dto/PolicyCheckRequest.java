package com.coniv.mait.web.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PolicyCheckRequest(

	@Schema(description = "정책 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "정책 ID는 필수입니다.")
	Long policyId,

	@Schema(description = "동의 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "동의 여부는 필수입니다.")
	boolean isChecked
) {
}
