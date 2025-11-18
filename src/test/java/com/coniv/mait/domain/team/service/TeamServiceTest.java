package com.coniv.mait.domain.team.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInviteEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInviteEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.component.InviteTokenGenerator;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.LoginProvider;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.enums.InviteTokenDuration;
import com.coniv.mait.global.exception.custom.TeamInviteFailException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

	@Mock
	private TeamEntityRepository teamEntityRepository;

	@Mock
	private TeamUserEntityRepository teamUserEntityRepository;

	@Mock
	private TeamInviteEntityRepository teamInviteEntityRepository;

	@Mock
	private InviteTokenGenerator inviteTokenGenerator;

	@Mock
	private UserEntityRepository userEntityRepository;

	@InjectMocks
	private TeamService teamService;

	@Test
	@DisplayName("팀 생성 시 TeamEntity와 Owner TeamUserEntity가 올바르게 저장된다")
	void createTeam_SavesTeamEntity() {
		// given
		String teamName = "테스트 팀";
		Long ownerId = 1L;
		UserEntity owner = mock(UserEntity.class);
		when(owner.getId()).thenReturn(ownerId);

		TeamEntity mockTeamEntity = TeamEntity.of(teamName, ownerId);
		TeamUserEntity ownerTeamUser = TeamUserEntity.createOwnerUser(owner, mockTeamEntity);

		when(userEntityRepository.findById(ownerId)).thenReturn(Optional.of(owner));
		when(teamEntityRepository.save(any(TeamEntity.class))).thenReturn(mockTeamEntity);
		when(teamUserEntityRepository.save(any(TeamUserEntity.class))).thenReturn(ownerTeamUser);

		// when
		teamService.createTeam(teamName, owner);

		// then
		verify(userEntityRepository).findById(ownerId);
		verify(teamEntityRepository, times(1)).save(any(TeamEntity.class));
		verify(teamUserEntityRepository, times(1)).save(any(TeamUserEntity.class));
	}

	@Test
	@DisplayName("사용자들과 팀 연결 시 TeamUserEntity들이 저장되는지 확인")
	void createUsersAndLinkTeam_SavesTeamUserEntities() {
		// given
		TeamEntity team = TeamEntity.of("테스트 팀", 1L);
		List<UserEntity> users = List.of(
			UserEntity.socialLoginUser("user1@test.com", "사용자1", "provider1", LoginProvider.GOOGLE),
			UserEntity.socialLoginUser("user2@test.com", "사용자2", "provider2", LoginProvider.GOOGLE)
		);

		// when
		teamService.createUsersAndLinkTeam(users, team);

		// then
		verify(teamUserEntityRepository, times(1)).saveAll(anyList());
	}

	@Test
	@DisplayName("팀 초대 코드 생성 성공 - Owner가 초대 코드를 생성한다")
	void createTeamInviteCode_Success_Owner() {
		// given
		Long teamId = 1L;
		Long ownerId = 1L;
		UserEntity mockOwner = mock(UserEntity.class);
		when(mockOwner.getId()).thenReturn(ownerId);
		TeamEntity team = TeamEntity.of("테스트 팀", 1L);
		TeamUserEntity ownerTeamUser = TeamUserEntity.createOwnerUser(mockOwner, team);
		String expectedToken = "test-invite-code";
		InviteTokenDuration duration = InviteTokenDuration.ONE_DAY;
		TeamUserRole role = TeamUserRole.PLAYER;

		when(teamEntityRepository.findById(teamId)).thenReturn(Optional.of(team));
		when(userEntityRepository.findById(ownerId)).thenReturn(Optional.of(mockOwner));
		when(teamUserEntityRepository.findByTeamAndUser(team, mockOwner)).thenReturn(Optional.of(ownerTeamUser));
		when(inviteTokenGenerator.generateUniqueInviteToken()).thenReturn(expectedToken);
		when(teamInviteEntityRepository.save(any(TeamInviteEntity.class))).thenAnswer(
			invocation -> invocation.getArgument(0));

		// when
		String result = teamService.createTeamInviteCode(teamId, mockOwner, duration, role);

		// then
		assertThat(result).isEqualTo(expectedToken);
		verify(teamEntityRepository).findById(teamId);
		verify(userEntityRepository).findById(ownerId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, mockOwner);
		verify(inviteTokenGenerator).generateUniqueInviteToken();
		verify(teamInviteEntityRepository).save(any(TeamInviteEntity.class));
	}

	@Test
	@DisplayName("팀 초대 코드 생성 실패 - 팀이 존재하지 않음")
	void createTeamInviteCode_Failure_TeamNotFound() {
		// given
		Long teamId = 999L;
		UserEntity mockUser = mock(UserEntity.class);
		InviteTokenDuration duration = InviteTokenDuration.ONE_DAY;
		TeamUserRole role = TeamUserRole.PLAYER;

		when(teamEntityRepository.findById(teamId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamService.createTeamInviteCode(teamId, mockUser, duration, role))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("Team not found with id: " + teamId);

		verify(teamEntityRepository).findById(teamId);
		verify(teamUserEntityRepository, never()).findByTeamAndUser(any(), any());
		verify(inviteTokenGenerator, never()).generateUniqueInviteToken();
		verify(teamInviteEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("팀 초대 코드 생성 실패 - 사용자가 팀 멤버가 아님")
	void createTeamInviteCode_Failure_NotTeamMember() {
		// given
		Long teamId = 1L;
		UserEntity mockUser = mock(UserEntity.class);
		when(mockUser.getId()).thenReturn(999L);
		TeamEntity team = TeamEntity.of("테스트 팀", 1L);
		InviteTokenDuration duration = InviteTokenDuration.ONE_DAY;
		TeamUserRole role = TeamUserRole.PLAYER;

		when(teamEntityRepository.findById(teamId)).thenReturn(Optional.of(team));
		when(userEntityRepository.findById(999L)).thenReturn(Optional.of(mockUser));
		when(teamUserEntityRepository.findByTeamAndUser(team, mockUser)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamService.createTeamInviteCode(teamId, mockUser, duration, role))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("Invitor is not a member of the team");

		verify(teamEntityRepository).findById(teamId);
		verify(userEntityRepository).findById(999L);
		verify(teamUserEntityRepository).findByTeamAndUser(team, mockUser);
		verify(inviteTokenGenerator, never()).generateUniqueInviteToken();
		verify(teamInviteEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("팀 초대 코드 생성 실패 - PLAYER는 초대 코드를 생성할 수 없음")
	void createTeamInviteCode_Failure_PlayerCannotInvite() {
		// given
		Long teamId = 1L;
		Long playerId = 2L;
		UserEntity mockPlayer = mock(UserEntity.class);
		when(mockPlayer.getId()).thenReturn(playerId);
		TeamEntity team = TeamEntity.of("테스트 팀", 1L);
		TeamUserEntity playerTeamUser = TeamUserEntity.createPlayerUser(mockPlayer, team);
		InviteTokenDuration duration = InviteTokenDuration.ONE_DAY;
		TeamUserRole role = TeamUserRole.PLAYER;

		when(teamEntityRepository.findById(teamId)).thenReturn(Optional.of(team));
		when(userEntityRepository.findById(playerId)).thenReturn(Optional.of(mockPlayer));
		when(teamUserEntityRepository.findByTeamAndUser(team, mockPlayer)).thenReturn(Optional.of(playerTeamUser));

		// when & then
		assertThatThrownBy(() -> teamService.createTeamInviteCode(teamId, mockPlayer, duration, role))
			.isInstanceOf(TeamInviteFailException.class)
			.hasMessageContaining("Only team owners can create invite codes");

		verify(teamEntityRepository).findById(teamId);
		verify(userEntityRepository).findById(playerId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, mockPlayer);
		verify(inviteTokenGenerator, never()).generateUniqueInviteToken();
		verify(teamInviteEntityRepository, never()).save(any());
	}
}
