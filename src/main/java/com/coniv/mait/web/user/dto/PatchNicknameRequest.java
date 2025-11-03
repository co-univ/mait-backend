package com.coniv.mait.web.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatchNicknameRequest(
	@NotNull(message = "닉네임 값은 필수입니다.")
	@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
	String nickname
) {
}
