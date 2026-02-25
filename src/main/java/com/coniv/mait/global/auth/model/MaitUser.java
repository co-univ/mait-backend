package com.coniv.mait.global.auth.model;

import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.Builder;

@Builder
public record MaitUser(

	Long id,

	String email,
	String name,
	String nickname,
	String nicknameCode,

	Boolean isLocalLogin
) {

	public static MaitUser from(final UserEntity userEntity) {
		return MaitUser.builder()
			.id(userEntity.getId())
			.email(userEntity.getEmail())
			.name(userEntity.getName())
			.nickname(userEntity.getNickname())
			.nicknameCode(userEntity.getNicknameCode())
			.isLocalLogin(userEntity.getIsLocalLogin())
			.build();
	}
}
