package com.coniv.mait.web.user.dto;

import com.coniv.mait.web.validation.TrimmedSize;

import jakarta.validation.constraints.NotNull;

public record UpdateNicknameRequest(
	@NotNull(message = "닉네임 값은 필수입니다.")
	@TrimmedSize(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
	String nickname
) {
}
