package com.coniv.mait.web.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RandomNicknameResponse(
	@Schema(description = "랜덤 닉네임", requiredMode = Schema.RequiredMode.REQUIRED)
	String nickname
) {
	public static RandomNicknameResponse from(String nickname) {
		return new RandomNicknameResponse(nickname);
	}
}
