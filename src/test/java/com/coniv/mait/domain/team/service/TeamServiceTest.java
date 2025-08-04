package com.coniv.mait.domain.team.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.LoginProvider;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

	@Mock
	private TeamEntityRepository teamEntityRepository;

	@Mock
	private TeamUserEntityRepository teamUserEntityRepository;

	@InjectMocks
	private TeamService teamService;

	@Test
	@DisplayName("팀 생성 시 TeamEntity가 저장되는지 확인")
	void createTeam_SavesTeamEntity() {
		// given
		String teamName = "테스트 팀";
		TeamEntity mockTeamEntity = TeamEntity.of(teamName);
		when(teamEntityRepository.save(any(TeamEntity.class))).thenReturn(mockTeamEntity);

		// when
		teamService.createTeam(teamName);

		// then
		verify(teamEntityRepository, times(1)).save(any(TeamEntity.class));
	}

	@Test
	@DisplayName("사용자들과 팀 연결 시 TeamUserEntity들이 저장되는지 확인")
	void createUsersAndLinkTeam_SavesTeamUserEntities() {
		// given
		TeamEntity team = TeamEntity.of("테스트 팀");
		List<UserEntity> users = List.of(
			UserEntity.socialLoginUser("user1@test.com", "사용자1", "provider1", LoginProvider.GOOGLE),
			UserEntity.socialLoginUser("user2@test.com", "사용자2", "provider2", LoginProvider.GOOGLE)
		);

		// when
		teamService.createUsersAndLinkTeam(users, team);

		// then
		verify(teamUserEntityRepository, times(1)).saveAll(anyList());
	}
}