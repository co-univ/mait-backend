package com.coniv.mait.domain.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.exception.custom.LoginFailException;
import com.coniv.mait.global.jwt.BlackList;
import com.coniv.mait.global.jwt.JwtTokenProvider;
import com.coniv.mait.global.jwt.RefreshToken;
import com.coniv.mait.global.jwt.Token;
import com.coniv.mait.global.jwt.cache.OauthAccessCodeRedisRepository;
import com.coniv.mait.global.jwt.repository.BlackListRepository;
import com.coniv.mait.global.jwt.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserEntityRepository userEntityRepository;

	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	private final JwtTokenProvider jwtTokenProvider;

	private final OauthAccessCodeRedisRepository oauthAccessCodeRedisRepository;

	private final BlackListRepository blackListRepository;
	private final RefreshTokenRepository refreshTokenRepository;

	public Token login(final String email, final String password) {
		UserEntity user = userEntityRepository.findByEmail(email)
			.orElseThrow(() -> new LoginFailException("해당 이메일을 가진 유저를 찾을 수 없습니다."));

		if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
			throw new LoginFailException("비밀번호가 일치하지 않습니다.");
		}
		return jwtTokenProvider.createToken(user.getId());
	}

	public String getAccessTokenFromCode(String code) {
		String mayBeAccessToken = oauthAccessCodeRedisRepository.findByCode(code);
		if (mayBeAccessToken == null) {
			throw new LoginFailException("유효하지 않은 코드입니다.");
		}
		return mayBeAccessToken;
	}

	public void logout(final String accessToken, final String refreshToken) {
		blackListRepository.save(BlackList.builder().id(accessToken).build());

		Long userId = jwtTokenProvider.getUserId(accessToken);
		refreshTokenRepository.deleteById(userId);

		blackListRepository.save(BlackList.builder().id(refreshToken).build());
	}

	public Token reissue(final String refreshToken) {
		jwtTokenProvider.validateRefreshToken(refreshToken);

		Long userId = jwtTokenProvider.getUserId(refreshToken);

		RefreshToken storedRefreshToken = refreshTokenRepository.findById(userId)
			.orElseThrow(() -> new LoginFailException("저장된 Refresh Token이 없습니다."));

		if (!storedRefreshToken.getRefreshToken().equals(refreshToken)) {
			throw new LoginFailException("Refresh Token이 일치하지 않습니다.");
		}

		blackListRepository.save(BlackList.builder().id(refreshToken).build());

		Token newToken = jwtTokenProvider.createToken(userId);

		refreshTokenRepository.save(new RefreshToken(userId, newToken.refreshToken()));

		return newToken;
	}
}
