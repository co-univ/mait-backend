package com.coniv.mait.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.user.component.UserNickNameGenerator;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.dto.UserDto;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserEntityRepository userEntityRepository;
	private final UserNickNameGenerator userNickNameGenerator;

	public UserDto getUserInfo(final UserEntity user) {
		return UserDto.builder()
			.id(user.getId())
			.email(user.getEmail())
			.name(user.getName())
			.nickname(user.getNickname())
			.build();
	}

	@Transactional
	public void updateUserNickname(final UserEntity ownerPrincipal, final String newNickname) {
		UserEntity user = userEntityRepository.findById(ownerPrincipal.getId())
			.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + ownerPrincipal.getId()));

		String code = userNickNameGenerator.generateNicknameCode(newNickname);
		user.updateNickname(newNickname, code);
	}
}
