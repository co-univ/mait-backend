package com.coniv.mait.web.user.dto;

import com.coniv.mait.domain.user.service.dto.UserDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PatchNicknameResponse(
	@Schema(description = "사용자 닉네임", requiredMode = Schema.RequiredMode.REQUIRED)
	String nickname,

	@Schema(description = "사용자 전체 닉네임", requiredMode = Schema.RequiredMode.REQUIRED)
	String fullNickname
) {
	public static PatchNicknameResponse from(UserDto dto) {
		return new PatchNicknameResponse(dto.getNickname(), dto.getFullNickname());
	}
}
