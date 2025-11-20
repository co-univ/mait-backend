package com.coniv.mait.web.user.dto;

import java.util.List;

import com.coniv.mait.web.validation.TrimmedSize;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SignUpApiRequest(
	@NotNull(message = "닉네임 값은 필수입니다.")
	@TrimmedSize(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
	String nickname,

	@NotEmpty(message = "정책 목록은 필수입니다.")
	List<PolicyCheckRequest> policyChecks
) {
}
