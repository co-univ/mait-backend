package com.coniv.mait.web.user.dto;

import com.coniv.mait.domain.user.service.dto.UserDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserInfoResponse(

	@Schema(description = "사용자 ID")
	Long id,

	@Schema(description = "사용자 이름")
	String name,

	@Schema(description = "사용자 이메일")
	String email,

	@Schema(description = "사용자 닉네임")
	String nickname
) {

	public static UserInfoResponse from(UserDto userDto) {
		return new UserInfoResponse(
			userDto.getId(),
			userDto.getName(),
			userDto.getEmail(),
			userDto.getNickname()
		);
	}
}
