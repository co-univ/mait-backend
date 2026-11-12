package com.coniv.mait.domain.user.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TeamRoleValidatorTest {

	@InjectMocks
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private UserReader userReader;

	@Mock
	private TeamReader teamReader;

	@Mock
	private TeamUserEntityRepository teamUserEntityRepository;

	@Mock
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	@ParameterizedTest
	@CsvSource({"OWNER, MAKING, MAKING, true", "MAKER, MAKING, MANAGING, true",
		"OWNER, ONGOING, MANAGING, true", "MAKER, MAKING, , true",
		"MAKER, ONGOING, LIVE_TIME, false", "OWNER, ONGOING, LIVE_TIME, false",
		"PLAYER, ONGOING, LIVE_TIME, false", "PLAYER, REVIEW, REVIEW, true",
		"MAKER, REVIEW, REVIEW, true", "OWNER, REVIEW, REVIEW, true"})
	@DisplayName("관리 화면은 정답을 공개하고 실시간 풀이 화면은 역할과 무관하게 숨긴다")
	void canViewQuestionAnswers_byRoleAndMode(TeamUserRole role, QuestionSetStatus status,
		DeliveryMode mode, boolean expected) {
		// given
		prepareAnswerAccess(role);
		QuestionSetEntity questionSet = QuestionSetEntity.builder().id(2L).teamId(1L)
			.solveMode(QuestionSetSolveMode.LIVE_TIME).status(status).build();

		// when & then
		assertThat(teamRoleValidator.canViewQuestionAnswers(questionSet, 100L, mode)).isEqualTo(expected);
		verifyNoInteractions(solvingSessionEntityRepository);
	}

	@ParameterizedTest
	@CsvSource({"PLAYER, false, PROGRESSING, false", "PLAYER, true, PROGRESSING, false",
		"PLAYER, true, COMPLETE, true", "MAKER, false, PROGRESSING, false",
		"MAKER, true, PROGRESSING, false", "MAKER, true, COMPLETE, true",
		"OWNER, false, PROGRESSING, false", "OWNER, true, PROGRESSING, false", "OWNER, true, COMPLETE, true"})
	@DisplayName("학습 문제의 정답은 요청자 본인의 채점 완료 세션이 있을 때 공개한다")
	void canViewQuestionAnswers_ownStudySession(TeamUserRole role, boolean exists, SolvingStatus status,
		boolean expected) {
		// given
		UserEntity user = prepareAnswerAccess(role);
		QuestionSetEntity questionSet = QuestionSetEntity.builder().id(2L).teamId(1L)
			.solveMode(QuestionSetSolveMode.STUDY).status(QuestionSetStatus.ONGOING).build();
		SolvingSessionEntity session = SolvingSessionEntity.studySession(user, questionSet);
		if (status == SolvingStatus.COMPLETE) {
			session.submit(4, 4);
		}
		Optional<SolvingSessionEntity> result = Optional.empty();
		if (exists) {
			result = Optional.of(session);
		}
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(100L, 2L,
			QuestionSetSolveMode.STUDY)).thenReturn(result);

		// when & then
		assertThat(teamRoleValidator.canViewQuestionAnswers(questionSet, 100L, DeliveryMode.STUDY)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({"PLAYER, 제작 중인 문제는 관리자만 조회할 수 있습니다.", ", 해당 팀의 멤버가 아닙니다."})
	@DisplayName("제작 중인 문제의 PLAYER 조회와 비멤버 조회를 거절한다")
	void canViewQuestionAnswers_forbidden(TeamUserRole role, String message) {
		// given
		prepareAnswerAccess(role);
		QuestionSetEntity questionSet = QuestionSetEntity.builder().id(2L).teamId(1L)
			.solveMode(QuestionSetSolveMode.STUDY).status(QuestionSetStatus.MAKING).build();

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.canViewQuestionAnswers(questionSet, 100L, DeliveryMode.STUDY))
			.isInstanceOf(UserRoleException.class).hasMessage(message);
		verifyNoInteractions(solvingSessionEntityRepository);
	}

	@ParameterizedTest
	@CsvSource({"ONGOING, MAKING", "ONGOING, MANAGING", "ONGOING, ",
		"REVIEW, MAKING", "REVIEW, MANAGING", "REVIEW, "})
	@DisplayName("PLAYER는 mode를 생략하거나 제작 및 관리 모드를 요청할 수 없다")
	void canViewQuestionAnswers_playerCannotRequestManagement(QuestionSetStatus status, DeliveryMode mode) {
		// given
		prepareAnswerAccess(TeamUserRole.PLAYER);
		QuestionSetEntity questionSet = QuestionSetEntity.builder().id(2L).teamId(1L)
			.solveMode(QuestionSetSolveMode.STUDY).status(status).build();

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.canViewQuestionAnswers(questionSet, 100L, mode))
			.isInstanceOf(UserRoleException.class).hasMessage("팀 관리 권한이 없습니다.");
		verifyNoInteractions(solvingSessionEntityRepository);
	}

	@ParameterizedTest
	@CsvSource({"LIVE_TIME, LIVE_TIME, BEFORE, ONLY_LIVE_TIME",
		"LIVE_TIME, LIVE_TIME, AFTER, ONLY_LIVE_TIME", "LIVE_TIME, LIVE_TIME, REVIEW, ONLY_LIVE_TIME",
		"LIVE_TIME, STUDY, ONGOING, ONLY_LIVE_TIME", "REVIEW, LIVE_TIME, BEFORE, ONLY_REVIEW",
		"REVIEW, LIVE_TIME, ONGOING, ONLY_REVIEW", "REVIEW, LIVE_TIME, AFTER, ONLY_REVIEW",
		"STUDY, LIVE_TIME, ONGOING, ONLY_STUDY"})
	@DisplayName("관리자도 실제 문제셋 상태와 다른 풀이 모드를 요청하면 거절한다")
	void canViewQuestionAnswers_invalidMode(DeliveryMode mode, QuestionSetSolveMode solveMode,
		QuestionSetStatus status, QuestionSetStatusExceptionCode code) {
		// given
		prepareAnswerAccess(TeamUserRole.MAKER);
		QuestionSetEntity questionSet = QuestionSetEntity.builder().id(2L).teamId(1L)
			.solveMode(solveMode).status(status).build();

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.canViewQuestionAnswers(questionSet, 100L, mode))
			.isInstanceOfSatisfying(QuestionSetStatusException.class,
				exception -> assertThat(exception.getExceptionCode()).isEqualTo(code));
		verifyNoInteractions(solvingSessionEntityRepository);
	}

	private UserEntity prepareAnswerAccess(TeamUserRole role) {
		TeamEntity team = TeamEntity.ofGroup("팀", 100L);
		UserEntity user = UserEntity.localLoginUser("user@test.com", "password", "사용자", "user");
		when(teamReader.getActiveTeam(1L)).thenReturn(team);
		when(userReader.getById(100L)).thenReturn(user);
		Optional<TeamUserEntity> membership = Optional.empty();
		if (role != null) {
			membership = Optional.of(TeamUserEntity.createTeamUser(user, team, role));
		}
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(membership);
		return user;
	}

	@Test
	@DisplayName("checkIsTeamManager - OWNER 권한을 가진 사용자는 팀 관리자 권한 확인 성공")
	void checkIsTeamManager_Owner_Success() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);
		TeamUserEntity teamUser = mock(TeamUserEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userReader.getById(userId)).thenReturn(user);
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.OWNER);

		// when & then
		assertThatCode(() -> teamRoleValidator.checkIsTeamManager(teamId, userId))
			.doesNotThrowAnyException();

		verify(teamReader).getActiveTeam(teamId);
		verify(userReader).getById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
		verify(teamUser).getUserRole();
	}

	@Test
	@DisplayName("checkIsTeamManager - MAKER 권한을 가진 사용자는 팀 관리자 권한 확인 성공")
	void checkIsTeamManager_Maker_Success() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);
		TeamUserEntity teamUser = mock(TeamUserEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userReader.getById(userId)).thenReturn(user);
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.MAKER);

		// when & then
		assertThatCode(() -> teamRoleValidator.checkIsTeamManager(teamId, userId))
			.doesNotThrowAnyException();

		verify(teamReader).getActiveTeam(teamId);
		verify(userReader).getById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
		verify(teamUser).getUserRole();
	}

	@Test
	@DisplayName("checkIsTeamManager - 존재하지 않는 팀인 경우 EntityNotFoundException 발생")
	void checkIsTeamManager_TeamNotFound_ThrowsException() {
		// given
		Long teamId = 999L;
		Long userId = 100L;

		when(teamReader.getActiveTeam(teamId)).thenThrow(new EntityNotFoundException(teamId + " : 해당 팀을 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamManager(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("해당 팀을 찾을 수 없습니다.");

		verify(teamReader).getActiveTeam(teamId);
		verify(userReader, never()).getById(anyLong());
		verify(teamUserEntityRepository, never()).findByTeamAndUser(any(), any());
	}

	@Test
	@DisplayName("checkIsTeamManager - 존재하지 않는 사용자인 경우 EntityNotFoundException 발생")
	void checkIsTeamManager_UserNotFound_ThrowsException() {
		// given
		Long teamId = 1L;
		Long userId = 999L;

		TeamEntity team = mock(TeamEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userReader.getById(userId)).thenThrow(new EntityNotFoundException("해당 유저를 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamManager(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("해당 유저를 찾을 수 없습니다.");

		verify(teamReader).getActiveTeam(teamId);
		verify(userReader).getById(userId);
		verify(teamUserEntityRepository, never()).findByTeamAndUser(any(), any());
	}

	@Test
	@DisplayName("checkIsTeamManager - 팀의 멤버가 아닌 경우 UserRoleException 발생")
	void checkIsTeamManager_NotTeamMember_ThrowsException() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userReader.getById(userId)).thenReturn(user);
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamManager(teamId, userId))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("해당 팀의 멤버가 아닙니다.");

		verify(teamReader).getActiveTeam(teamId);
		verify(userReader).getById(userId);
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
		when(userReader.getById(userId)).thenReturn(user);
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

		verify(userReader, never()).getById(anyLong());
	}

	@Test
	@DisplayName("checkIsTeamMember - 존재하지 않는 사용자인 경우 EntityNotFoundException 발생")
	void checkIsTeamMember_UserNotFound_ThrowsException() {
		// given
		Long teamId = 1L;
		Long userId = 999L;

		TeamEntity team = mock(TeamEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userReader.getById(userId)).thenThrow(new EntityNotFoundException("해당 유저를 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamMember(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("해당 유저를 찾을 수 없습니다.");
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
		when(userReader.getById(userId)).thenReturn(user);
		when(teamUserEntityRepository.existsByTeamAndUser(team, user)).thenReturn(false);

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamMember(teamId, userId))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("해당 팀의 멤버가 아닙니다.");
	}

	@Test
	@DisplayName("checkIsTeamManager - PLAYER 권한을 가진 사용자는 UserRoleException 발생")
	void checkIsTeamManager_Player_ThrowsUserRoleException() {
		// given
		Long teamId = 1L;
		Long userId = 100L;

		TeamEntity team = mock(TeamEntity.class);
		UserEntity user = mock(UserEntity.class);
		TeamUserEntity teamUser = mock(TeamUserEntity.class);

		when(teamReader.getActiveTeam(teamId)).thenReturn(team);
		when(userReader.getById(userId)).thenReturn(user);
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.PLAYER);

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamManager(teamId, userId))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("팀 관리 권한이 없습니다.");

		verify(teamReader).getActiveTeam(teamId);
		verify(userReader).getById(userId);
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
		when(userReader.getById(userId)).thenReturn(user);
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.OWNER);

		// when & then
		assertThatCode(() -> teamRoleValidator.checkIsTeamOwner(teamId, userId))
			.doesNotThrowAnyException();

		verify(teamReader).getActiveTeam(teamId);
		verify(userReader).getById(userId);
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
		when(userReader.getById(userId)).thenReturn(user);
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.of(teamUser));
		when(teamUser.getUserRole()).thenReturn(TeamUserRole.MAKER);

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamOwner(teamId, userId))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("해당 팀의 OWNER가 아닙니다.");

		verify(teamReader).getActiveTeam(teamId);
		verify(userReader).getById(userId);
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
		when(userReader.getById(userId)).thenReturn(user);
		when(teamUserEntityRepository.findByTeamAndUser(team, user)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> teamRoleValidator.checkIsTeamOwner(teamId, userId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("해당 팀의 멤버가 아닙니다.");

		verify(teamReader).getActiveTeam(teamId);
		verify(userReader).getById(userId);
		verify(teamUserEntityRepository).findByTeamAndUser(team, user);
	}
}
