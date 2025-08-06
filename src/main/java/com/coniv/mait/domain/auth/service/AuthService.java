package com.coniv.mait.domain.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.exception.custom.LoginFailException;
import com.coniv.mait.global.jwt.JwtTokenProvider;
import com.coniv.mait.global.jwt.Token;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserEntityRepository userEntityRepository;

	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	private final JwtTokenProvider jwtTokenProvider;

	public Token login(final String email, final String password) {
		UserEntity user = userEntityRepository.findByEmail(email)
			.orElseThrow(() -> new LoginFailException("해당 이메일을 가진 유저를 찾을 수 없습니다."));

		if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
			throw new LoginFailException("비밀번호가 일치하지 않습니다.");
		}
		return jwtTokenProvider.createToken(user.getId());
	}
}
