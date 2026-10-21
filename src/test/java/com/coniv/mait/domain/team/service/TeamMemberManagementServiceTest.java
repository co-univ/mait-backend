package com.coniv.mait.domain.team.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.exception.TeamManagerException;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;

@ExtendWith(MockitoExtension.class)
class TeamMemberManagementServiceTest {

	private static final Long TEAM_ID = 10L;
	private static final Long REQUESTER_ID = 100L;
	private static final Long TARGET_USER_ID = 200L;
	private static final Long TEAM_USER_ID = 20L;

	@Mock
	private TeamUserEntityRepository teamUserEntityRepository;

	@Mock
	private TeamReader teamReader;

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@InjectMocks
	private TeamService teamService;

	private TeamEntity team;
	private UserEntity targetUser;
	private TeamUserEntity targetMember;

	@BeforeEach
	void setUp() {
		team = TeamEntity.ofGroup("팀", REQUESTER_ID);
		ReflectionTestUtils.setField(team, "id", TEAM_ID);
		targetUser = UserEntity.localLoginUser("target@example.com", "password", "대상", "대상");
		targetMember = TeamUserEntity.createPlayerUser(targetUser, team);
	}

	@Test
	@DisplayName("팀원 추가는 요청자의 관리 권한 확인 후 대상 사용자를 등록한다")
	void addUserAfterCheckingRequester() {
		// given
		when(teamReader.getActiveTeam(TEAM_ID)).thenReturn(team);
		when(userEntityRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));

		// when
		teamService.addUserInTeam(TEAM_ID, TARGET_USER_ID, TeamUserRole.MAKER, REQUESTER_ID);

		// then
		var order = inOrder(teamRoleValidator, userEntityRepository, teamUserEntityRepository);
		order.verify(teamRoleValidator).checkHasManageTeamMemberAuthority(TEAM_ID, REQUESTER_ID);
		order.verify(userEntityRepository).findById(TARGET_USER_ID);
		ArgumentCaptor<TeamUserEntity> captor = ArgumentCaptor.forClass(TeamUserEntity.class);
		order.verify(teamUserEntityRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isSameAs(targetUser);
		assertThat(captor.getValue().getTeam()).isSameAs(team);
		assertThat(captor.getValue().getUserRole()).isEqualTo(TeamUserRole.MAKER);
	}

	@Test
	@DisplayName("팀원 삭제는 대상 팀에 대한 요청자 권한 확인 후 수행한다")
	void deleteUserAfterCheckingTargetTeam() {
		// given
		when(teamUserEntityRepository.findById(TEAM_USER_ID)).thenReturn(Optional.of(targetMember));

		// when
		teamService.deleteTeamUser(TEAM_USER_ID, REQUESTER_ID);

		// then
		var order = inOrder(teamRoleValidator, teamUserEntityRepository);
		order.verify(teamRoleValidator).checkHasManageTeamMemberAuthority(TEAM_ID, REQUESTER_ID);
		order.verify(teamUserEntityRepository).delete(targetMember);
	}

	@Test
	@DisplayName("팀원 역할 변경은 대상 팀에 대한 요청자 권한을 검사한다")
	void updateRoleAfterCheckingTargetTeam() {
		// given
		when(teamUserEntityRepository.findById(TEAM_USER_ID)).thenReturn(Optional.of(targetMember));

		// when
		teamService.updateTeamUserRole(TEAM_USER_ID, TeamUserRole.MAKER, REQUESTER_ID);

		// then
		verify(teamRoleValidator).checkHasManageTeamMemberAuthority(TEAM_ID, REQUESTER_ID);
		assertThat(targetMember.getUserRole()).isEqualTo(TeamUserRole.MAKER);
	}

	@ParameterizedTest
	@ValueSource(strings = {"ADD", "DELETE", "UPDATE"})
	@DisplayName("관리 권한이 없으면 팀원 추가·삭제·역할 변경의 부수 효과가 없다")
	void unauthorizedRequesterCannotMutateMembers(String operation) {
		// given
		stubTarget(operation);
		doThrow(new UserRoleException("팀원을 관리할 권한이 없습니다."))
			.when(teamRoleValidator).checkHasManageTeamMemberAuthority(TEAM_ID, REQUESTER_ID);

		// when & then
		assertThatThrownBy(() -> mutate(operation, TeamUserRole.MAKER))
			.isInstanceOf(UserRoleException.class).hasMessage("팀원을 관리할 권한이 없습니다.");
		verify(teamUserEntityRepository, never()).save(any());
		verify(teamUserEntityRepository, never()).delete(any());
		verifyNoInteractions(userEntityRepository);
		assertThat(targetMember.getUserRole()).isEqualTo(TeamUserRole.PLAYER);
	}

	@ParameterizedTest
	@ValueSource(strings = {"DELETE", "UPDATE"})
	@DisplayName("팀 관리자는 OWNER를 삭제하거나 역할을 바꿀 수 없다")
	void cannotMutateOwner(String operation) {
		// given
		targetMember = TeamUserEntity.createOwnerUser(targetUser, team);
		stubTarget(operation);
		String message = operation.equals("DELETE")
			? "Cannot delete OWNER from the team." : "Cannot change role of OWNER.";

		// when & then
		assertThatThrownBy(() -> mutate(operation, TeamUserRole.MAKER))
			.isInstanceOf(TeamManagerException.class).hasMessage(message);
		verify(teamRoleValidator).checkHasManageTeamMemberAuthority(TEAM_ID, REQUESTER_ID);
		verify(teamUserEntityRepository, never()).delete(any());
		assertThat(targetMember.getUserRole()).isEqualTo(TeamUserRole.OWNER);
	}

	@ParameterizedTest
	@ValueSource(strings = {"ADD", "UPDATE"})
	@DisplayName("팀원 추가·역할 변경으로 OWNER를 새로 부여할 수 없다")
	void cannotAssignOwner(String operation) {
		// given
		stubTarget(operation);
		String message = operation.equals("ADD")
			? "Cannot add team user with OWNER role." : "Cannot set team user role to OWNER.";

		// when & then
		assertThatThrownBy(() -> mutate(operation, TeamUserRole.OWNER))
			.isInstanceOf(TeamManagerException.class).hasMessage(message);
		verify(teamUserEntityRepository, never()).save(any());
		assertThat(targetMember.getUserRole()).isEqualTo(TeamUserRole.PLAYER);
	}

	@ParameterizedTest
	@ValueSource(strings = {"ADD", "DELETE", "UPDATE"})
	@DisplayName("삭제된 팀에서는 팀원 관리가 실행되지 않는다")
	void cannotMutateDeletedTeam(String operation) {
		// given
		if (operation.equals("ADD")) {
			when(teamReader.getActiveTeam(TEAM_ID)).thenThrow(new TeamManagerException("삭제된 팀입니다."));
		} else {
			stubTarget(operation);
			doThrow(new TeamManagerException("삭제된 팀입니다.")).when(teamReader).validateActiveTeam(team);
		}

		// when & then
		assertThatThrownBy(() -> mutate(operation, TeamUserRole.MAKER))
			.isInstanceOf(TeamManagerException.class).hasMessage("삭제된 팀입니다.");
		verifyNoInteractions(teamRoleValidator, userEntityRepository);
		verify(teamUserEntityRepository, never()).save(any());
		verify(teamUserEntityRepository, never()).delete(any());
		assertThat(targetMember.getUserRole()).isEqualTo(TeamUserRole.PLAYER);
	}

	private void stubTarget(String operation) {
		if (operation.equals("ADD")) {
			when(teamReader.getActiveTeam(TEAM_ID)).thenReturn(team);
		} else {
			when(teamUserEntityRepository.findById(TEAM_USER_ID)).thenReturn(Optional.of(targetMember));
		}
	}

	private void mutate(String operation, TeamUserRole role) {
		switch (operation) {
			case "ADD" -> teamService.addUserInTeam(TEAM_ID, TARGET_USER_ID, role, REQUESTER_ID);
			case "DELETE" -> teamService.deleteTeamUser(TEAM_USER_ID, REQUESTER_ID);
			case "UPDATE" -> teamService.updateTeamUserRole(TEAM_USER_ID, role, REQUESTER_ID);
			default -> throw new IllegalArgumentException(operation);
		}
	}
}
