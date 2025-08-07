package com.coniv.mait.domain.user.service;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.service.dto.UserDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	public UserDto getUserInfo(final UserEntity user) {
		return UserDto.builder()
			.id(user.getId())
			.email(user.getEmail())
			.name(user.getName())
			.nickname(user.getNickname())
			.build();
	}
}
