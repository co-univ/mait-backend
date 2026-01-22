package com.coniv.mait.domain.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.auth.dto.OauthPendingPayload;
import com.coniv.mait.domain.user.component.UserNickNameGenerator;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.LoginProvider;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.dto.UserDto;
import com.coniv.mait.domain.user.util.RandomNicknameUtil;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;
import com.coniv.mait.global.auth.jwt.RefreshToken;
import com.coniv.mait.global.auth.jwt.Token;
import com.coniv.mait.global.auth.jwt.cache.OauthPendingRedisRepository;
import com.coniv.mait.global.auth.jwt.repository.RefreshTokenRepository;
import com.coniv.mait.web.user.dto.PolicyCheckRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserEntityRepository userEntityRepository;
	private final UserNickNameGenerator userNickNameGenerator;
	private final PolicyService policyService;
	private final OauthPendingRedisRepository oauthPendingRedisRepository;
	private final ObjectMapper objectMapper;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;

	public UserDto getUserInfo(final UserEntity user) {
		return UserDto.from(user);
	}

	@Transactional
	public UserDto updateUserNickname(final UserEntity ownerPrincipal, final String newNickname) {
		UserEntity user = userEntityRepository.findById(ownerPrincipal.getId())
			.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + ownerPrincipal.getId()));

		String code = userNickNameGenerator.generateNicknameCode(newNickname);
		user.updateNickname(newNickname, code);

		return UserDto.from(user);
	}

	public String getRandomNickname() {
		return RandomNicknameUtil.generateRandomNickname();
	}

	@Transactional
	public Token signup(String signupToken, String nickname, List<PolicyCheckRequest> policyChecks) {
		String value = oauthPendingRedisRepository.findByKey(signupToken);
		if (value == null) {
			throw new EntityNotFoundException("Signup token not found or expired");
		}

		OauthPendingPayload payload;
		try {
			payload = objectMapper.readValue(value, OauthPendingPayload.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse pending signup payload", e);
		}

		LoginProvider provider = LoginProvider.findByProvider(payload.getProvider());
		UserEntity user = UserEntity.socialLoginUser(payload.getEmail(), payload.getName(), payload.getProviderId(),
			provider);

		String code = userNickNameGenerator.generateNicknameCode(nickname);
		user.updateNickname(nickname, code);
		UserEntity saved = userEntityRepository.save(user);

		policyService.checkPolicies(saved.getId(), policyChecks);

		oauthPendingRedisRepository.deleteByKey(signupToken);

		Token token = jwtTokenProvider.createToken(saved.getId());
		RefreshToken refreshToken = new RefreshToken(saved.getId(), token.refreshToken());
		refreshTokenRepository.save(refreshToken);
		return token;
	}

	@Transactional(readOnly = true)
	public List<UserDto> findUserByEmail(String email) {
		return userEntityRepository.findAllByEmail(email).stream()
			.map(UserDto::from)
			.toList();
	}
}
