package com.coniv.mait.domain.user.service.dto;

import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
	private Long id;

	private String email;

	private String name;

	private String nickname;

	private String fullNickname;

	public static UserDto from(UserEntity user) {
		return UserDto.builder()
			.id(user.getId())
			.email(user.getEmail())
			.name(user.getName())
			.nickname(user.getNickname())
			.fullNickname(user.getFullNickname())
			.build();
	}
}
