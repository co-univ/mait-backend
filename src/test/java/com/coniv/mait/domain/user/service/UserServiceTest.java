package com.coniv.mait.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.auth.dto.OauthPendingPayload;
import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.user.component.UserNickNameGenerator;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.dto.UserDto;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;
import com.coniv.mait.global.auth.jwt.Token;
import com.coniv.mait.global.auth.jwt.cache.OauthPendingRedisRepository;
import com.coniv.mait.global.auth.jwt.repository.RefreshTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private UserNickNameGenerator userNickNameGenerator;

	@Mock
	private PolicyService policyService;

	@Mock
	private OauthPendingRedisRepository oauthPendingRedisRepository;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private TeamService teamService;

	@InjectMocks
	private UserService userService;

	@Test
	@DisplayName("닉네임 업데이트 성공 - 사용자의 닉네임과 코드가 올바르게 업데이트된다")
	void updateUserNickname_Success() {
		// given
		Long userId = 1L;
		String newNickname = "새로운닉네임";
		String generatedCode = "1234";

		UserEntity mockUser = mock(UserEntity.class);
		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.getNickname()).thenReturn(newNickname);
		when(mockUser.getNicknameCode()).thenReturn(generatedCode);

		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
		when(userNickNameGenerator.generateNicknameCode(newNickname)).thenReturn(generatedCode);

		// when
		UserDto result = userService.updateUserNickname(userId, newNickname);

		// then
		verify(userEntityRepository).findById(userId);
		verify(userNickNameGenerator).generateNicknameCode(newNickname);
		verify(mockUser).updateNickname(newNickname, generatedCode);

		assertThat(result.getNickname()).isEqualTo(newNickname);
		assertThat(result.getNicknameCode()).isEqualTo(generatedCode);
	}

	@Test
	@DisplayName("닉네임 업데이트 실패 - 사용자가 존재하지 않음")
	void updateUserNickname_Failure_UserNotFound() {
		// given
		Long userId = 999L;
		String newNickname = "새로운닉네임";

		UserEntity mockUser = mock(UserEntity.class);

		when(userEntityRepository.findById(userId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userService.updateUserNickname(userId, newNickname))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("User not found with id: " + userId);

		verify(userEntityRepository).findById(userId);
		verify(userNickNameGenerator, never()).generateNicknameCode(any());
	}

	@Test
	@DisplayName("랜덤 닉네임 생성 - 형용사와 명사의 조합으로 닉네임이 생성된다")
	void getRandomNickname_ReturnsRandomNickname() {
		// when
		String randomNickname = userService.getRandomNickname();

		// then
		assertThat(randomNickname).isNotNull();
		assertThat(randomNickname).isNotEmpty();
		assertThat(randomNickname.length()).isGreaterThanOrEqualTo(3);
	}

	@Test
	@DisplayName("회원가입 성공 - 저장된 유저로 개인 워크스페이스가 생성되고 토큰이 발급된다")
	void signup_Success_CreatesPersonalWorkspace() throws Exception {
		// given
		String signupToken = "signup-token";
		String nickname = "신규유저";
		String nicknameCode = "0001";
		String pendingJson = "{\"provider\":\"google\",\"providerId\":\"pid\","
			+ "\"email\":\"new@example.com\",\"name\":\"홍길동\"}";

		OauthPendingPayload payload = OauthPendingPayload.builder()
			.provider("google")
			.providerId("pid")
			.email("new@example.com")
			.name("홍길동")
			.build();

		UserEntity savedUser = mock(UserEntity.class);
		when(savedUser.getId()).thenReturn(10L);

		Token expectedToken = new Token("access", "refresh");

		when(oauthPendingRedisRepository.findByKey(signupToken)).thenReturn(pendingJson);
		when(objectMapper.readValue(pendingJson, OauthPendingPayload.class)).thenReturn(payload);
		when(userNickNameGenerator.generateNicknameCode(nickname)).thenReturn(nicknameCode);
		when(userEntityRepository.save(any(UserEntity.class))).thenReturn(savedUser);
		when(jwtTokenProvider.createToken(10L)).thenReturn(expectedToken);

		// when
		Token result = userService.signup(signupToken, nickname, List.of());

		// then
		assertThat(result).isEqualTo(expectedToken);

		ArgumentCaptor<UserEntity> workspaceOwnerCaptor = ArgumentCaptor.forClass(UserEntity.class);
		verify(teamService).createPersonalWorkspace(workspaceOwnerCaptor.capture());
		assertThat(workspaceOwnerCaptor.getValue()).isSameAs(savedUser);

		verify(policyService).checkPolicies(eq(10L), anyList());
		verify(oauthPendingRedisRepository).deleteByKey(signupToken);
		verify(refreshTokenRepository).save(any());
	}

	@Test
	@DisplayName("회원가입 실패 - 가입 토큰이 만료되었거나 존재하지 않으면 예외 발생")
	void signup_Failure_SignupTokenNotFound() {
		// given
		String signupToken = "expired-token";
		when(oauthPendingRedisRepository.findByKey(signupToken)).thenReturn(null);

		// when & then
		assertThatThrownBy(() -> userService.signup(signupToken, "닉네임", List.of()))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("Signup token not found or expired");

		verify(userEntityRepository, never()).save(any());
		verify(teamService, never()).createPersonalWorkspace(any());
	}
}
