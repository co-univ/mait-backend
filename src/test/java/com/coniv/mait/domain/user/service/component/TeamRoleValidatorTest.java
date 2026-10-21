package com.coniv.mait.domain.user.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.component.TeamReader;
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
	private TeamReader teamReader;

	@Mock
	private TeamUserEntityRepository teamUserEntityRepository;

	@ParameterizedTest
	@EnumSource(value = TeamUserRole.class, names = {"OWNER", "MAKER"})
	@DisplayName("해당 팀의 OWNER와 MAKER는 팀원 관리 권한을 가진다")
	void checkManageTeamMemberAuthority_success(TeamUserRole role) {
		// given
		TeamEntity team = TeamEntity.ofGroup("팀", 100L);
		UserEntity user = UserEntity.localLoginUser("manager@example.com", "password", "관리자", "관리자");
		when(teamReader.getActiveTeam(1L)).thenReturn(team);
		when(userEntityRepository.findById(100L)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user))
			.thenReturn(Optional.of(TeamUserEntity.createTeamUser(user, team, role)));

		// when & then
		assertThatCode(() -> teamRoleValidator.checkHasManageTeamMemberAuthority(1L, 100L))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("PLAYER는 팀원 관리 권한을 가지지 않는다")
	void checkManageTeamMemberAuthority_playerDenied() {
		// given
		TeamEntity team = TeamEntity.ofGroup("팀", 100L);
		UserEntity user = UserEntity.localLoginUser("player@example.com", "password", "참가자", "참가자");
		when(teamReader.getActiveTeam(1L)).thenReturn(team);
		when(userEntityRepository.findById(100L)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user))
			.thenReturn(Optional.of(TeamUserEntity.createPlayerUser(user, team)));

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasManageTeamMemberAuthority(1L, 100L))
			.isInstanceOf(UserRoleException.class).hasMessage("팀원을 관리할 권한이 없습니다.");
	}

	@Test
	@DisplayName("조회한 대상 팀에 소속되지 않은 요청자는 팀원 관리를 거절한다")
	void checkManageTeamMemberAuthority_nonMemberDenied() {
		// given
		TeamEntity targetTeam = TeamEntity.ofGroup("대상 팀", 200L);
		UserEntity user = UserEntity.localLoginUser("other-owner@example.com", "password", "다른팀 오너", "오너");
		when(teamReader.getActiveTeam(1L)).thenReturn(targetTeam);
		when(userEntityRepository.findById(100L)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(targetTeam, user)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasManageTeamMemberAuthority(1L, 100L))
			.isInstanceOf(UserRoleException.class).hasMessage("해당 팀의 멤버가 아닙니다.");
	}

	@Test
	@DisplayName("존재하지 않는 요청자는 팀원 관리 권한 확인을 통과할 수 없다")
	void checkManageTeamMemberAuthority_missingUserDenied() {
		// given
		when(teamReader.getActiveTeam(1L)).thenReturn(TeamEntity.ofGroup("팀", 200L));
		when(userEntityRepository.findById(100L)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasManageTeamMemberAuthority(1L, 100L))
			.isInstanceOf(EntityNotFoundException.class).hasMessage("존재하지 않는 사용자입니다.");
		verifyNoInteractions(teamUserEntityRepository);
	}

	@Test
	@DisplayName("checkHasCreateQuestionSetAuthority - OWNER 권한을 가진 사용자는 문제 세트 생성 권한 확인 성공")
	void checkHasCreateQuestionSetAuthority_Owner_Success() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);
		TeamUserEntity teamUser = mock(TeamUserEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.OWNER);

		// when & then
		assertThatCode(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.doesNotThrowAnyException();

		verify(teamReader).getActiveTeam(teamId);
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

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.MAKER);

		// when & then
		assertThatCode(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.doesNotThrowAnyException();

		verify(teamReader).getActiveTeam(teamId);
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

		when(teamReader.getActiveTeam(teamId)).thenThrow(new EntityNotFoundException(teamId + " : 해당 팀을 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("해당 팀을 찾을 수 없습니다.");

		verify(teamReader).getActiveTeam(teamId);
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

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("존재하지 않는 사용자입니다.");

		verify(teamReader).getActiveTeam(teamId);
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

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("해당 팀의 멤버가 아닙니다.");

		verify(teamReader).getActiveTeam(teamId);
		verify(userEntityRepository).findById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
	}

	@Test
	@DisplayName("checkIsTeamMember - 팀 멤버인 경우 예외 없이 통과")
	void checkIsTeamMember_Success() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.existsByTeamAndUser(team, user)).thenReturn(true);

		// when & then
		assertThatCode(() -> teamRoleValidator.checkIsTeamMember(teamId, userId))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("checkIsTeamMember - 존재하지 않는 팀인 경우 EntityNotFoundException 발생")
	void checkIsTeamMember_TeamNotFound_ThrowsException() {
		// given
		Long teamId = 999L;
		Long userId = 100L;

		when(teamReader.getActiveTeam(teamId)).thenThrow(new EntityNotFoundException(teamId + " : 해당 팀을 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamMember(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("해당 팀을 찾을 수 없습니다.");

		verify(userEntityRepository, never()).findById(anyLong());
	}

	@Test
	@DisplayName("checkIsTeamMember - 존재하지 않는 사용자인 경우 EntityNotFoundException 발생")
	void checkIsTeamMember_UserNotFound_ThrowsException() {
		// given
		Long teamId = 1L;
		Long userId = 999L;

		TeamEntity team = mock(TeamEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamMember(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("존재하지 않는 사용자입니다.");
	}

	@Test
	@DisplayName("checkIsTeamMember - 팀 멤버가 아닌 경우 UserRoleException 발생")
	void checkIsTeamMember_NotMember_ThrowsException() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.existsByTeamAndUser(team, user)).thenReturn(false);

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamMember(teamId, userId))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("해당 팀의 멤버가 아닙니다.");
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

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.PLAYER);

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("문제 세트 생성 권한이 없습니다.");

		verify(teamReader).getActiveTeam(teamId);
		verify(userEntityRepository).findById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
		verify(teamUser).getUserRole();
	}

	@Test
	@DisplayName("checkIsTeamOwner - OWNER 권한을 가진 사용자는 팀 OWNER 확인 성공")
	void checkIsTeamOwner_Owner_Success() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);
		TeamUserEntity teamUser = mock(TeamUserEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.OWNER);

		// when & then
		assertThatCode(() -> teamRoleValidator.checkIsTeamOwner(teamId, userId))
			.doesNotThrowAnyException();

		verify(teamReader).getActiveTeam(teamId);
		verify(userEntityRepository).findById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
		verify(teamUser).getUserRole();
	}

	@Test
	@DisplayName("checkIsTeamOwner - MAKER 권한을 가진 사용자는 UserRoleException 발생")
	void checkIsTeamOwner_Maker_ThrowsUserRoleException() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);
		TeamUserEntity teamUser = mock(TeamUserEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.MAKER);

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamOwner(teamId, userId))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("해당 팀의 OWNER가 아닙니다.");

		verify(teamReader).getActiveTeam(teamId);
		verify(userEntityRepository).findById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
		verify(teamUser).getUserRole();
	}

	@Test
	@DisplayName("checkIsTeamOwner - 팀 멤버가 아닌 경우 EntityNotFoundException 발생")
	void checkIsTeamOwner_NotTeamMember_ThrowsException() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(user));
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamOwner(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("해당 팀의 멤버가 아닙니다.");

		verify(teamReader).getActiveTeam(teamId);
		verify(userEntityRepository).findById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
	}
}
