package com.coniv.mait.domain.user.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.repository.UserEntityRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TeamRoleValidatorTest {

	@InjectMocks
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private TeamEntityRepository teamEntityRepository;

	@Mock
	private TeamUserEntityRepository teamUserEntityRepository;

	@Test
	@DisplayName("checkHasCreateQuestionSetAuthority - OWNER 권한을 가진 사용자는 문제 세트 생성 권한 확인 성공")
	void checkHasCreateQuestionSetAuthority_Owner_Success() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);
		TeamUserEntity teamUser = mock(TeamUserEntity.class);

		when(teamEntityRepository.findById(teamId)).thenReturn(Optional.of(team));
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.OWNER);

		// when & then
		assertThatCode(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.doesNotThrowAnyException();

		verify(teamEntityRepository).findById(teamId);
		verify(userEntityRepository).findById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
		verify(teamUser).getUserRole();
	}

	@Test
	@DisplayName("checkHasCreateQuestionSetAuthority - MAKER 권한을 가진 사용자는 문제 세트 생성 권한 확인 성공")
	void checkHasCreateQuestionSetAuthority_Maker_Success() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);
		TeamUserEntity teamUser = mock(TeamUserEntity.class);

		when(teamEntityRepository.findById(teamId)).thenReturn(Optional.of(team));
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.MAKER);

		// when & then
		assertThatCode(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.doesNotThrowAnyException();

		verify(teamEntityRepository).findById(teamId);
		verify(userEntityRepository).findById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
		verify(teamUser).getUserRole();
	}

	@Test
	@DisplayName("checkHasCreateQuestionSetAuthority - 존재하지 않는 팀인 경우 EntityNotFoundException 발생")
	void checkHasCreateQuestionSetAuthority_TeamNotFound_ThrowsException() {
		// given
		Long teamId = 999L;
		Long userId = 100L;

		when(teamEntityRepository.findById(teamId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("존재하지 않는 팀입니다.");

		verify(teamEntityRepository).findById(teamId);
		verify(userEntityRepository, never()).findById(anyLong());
		verify(teamUserEntityRepository, never()).findByTeamAndUser(any(), any());
	}

	@Test
	@DisplayName("checkHasCreateQuestionSetAuthority - 존재하지 않는 사용자인 경우 EntityNotFoundException 발생")
	void checkHasCreateQuestionSetAuthority_UserNotFound_ThrowsException() {
		// given
		Long teamId = 1L;
		Long userId = 999L;

		TeamEntity team = mock(TeamEntity.class);

		when(teamEntityRepository.findById(teamId)).thenReturn(Optional.of(team));
		when(userEntityRepository.findById(userId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("존재하지 않는 사용자입니다.");

		verify(teamEntityRepository).findById(teamId);
		verify(userEntityRepository).findById(userId);
		verify(teamUserEntityRepository, never()).findByTeamAndUser(any(), any());
	}

	@Test
	@DisplayName("checkHasCreateQuestionSetAuthority - 팀의 멤버가 아닌 경우 EntityNotFoundException 발생")
	void checkHasCreateQuestionSetAuthority_NotTeamMember_ThrowsException() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);

		when(teamEntityRepository.findById(teamId)).thenReturn(Optional.of(team));
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("해당 팀의 멤버가 아닙니다.");

		verify(teamEntityRepository).findById(teamId);
		verify(userEntityRepository).findById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
	}

	@Test
	@DisplayName("checkHasCreateQuestionSetAuthority - PLAYER 권한을 가진 사용자는 UserRoleException 발생")
	void checkHasCreateQuestionSetAuthority_Player_ThrowsUserRoleException() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);
		TeamUserEntity teamUser = mock(TeamUserEntity.class);

		when(teamEntityRepository.findById(teamId)).thenReturn(Optional.of(team));
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.PLAYER);

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("문제 세트 생성 권한이 없습니다.");

		verify(teamEntityRepository).findById(teamId);
		verify(userEntityRepository).findById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
		verify(teamUser).getUserRole();
	}
}
