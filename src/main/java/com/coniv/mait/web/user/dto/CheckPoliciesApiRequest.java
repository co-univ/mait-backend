package com.coniv.mait.web.user.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CheckPoliciesApiRequest(

	@Schema(description = "체크할 정책 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotEmpty(message = "정책 목록은 필수입니다.")
	@Valid
	List<PolicyCheckRequest> policyChecks
) {
}
