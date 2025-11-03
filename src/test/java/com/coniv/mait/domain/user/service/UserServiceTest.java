package com.coniv.mait.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.user.component.UserNickNameGenerator;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.dto.UserDto;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private UserNickNameGenerator userNickNameGenerator;

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
		UserDto result = userService.updateUserNickname(mockUser, newNickname);

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
		when(mockUser.getId()).thenReturn(userId);

		when(userEntityRepository.findById(userId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userService.updateUserNickname(mockUser, newNickname))
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
		assertThat(randomNickname.length()).isGreaterThanOrEqualTo(4);
	}
}
