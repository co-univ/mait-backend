package com.coniv.mait.web.user.dto;

import com.coniv.mait.domain.user.service.dto.UserDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserInfoApiResponse(

	@Schema(description = "사용자 ID")
	Long id,

	@Schema(description = "사용자 이름")
	String name,

	@Schema(description = "사용자 이메일")
	String email,

	@Schema(description = "사용자 닉네임")
	String nickname,

	@Schema(description = "사용자 전체 닉네임")
	String fullNickname
) {

	public static UserInfoApiResponse from(UserDto userDto) {
		if (userDto == null) {
			return null;
		}
		return new UserInfoApiResponse(
			userDto.getId(),
			userDto.getName(),
			userDto.getEmail(),
			userDto.getNickname(),
			userDto.getFullNickname()
		);
	}
}
