package com.coniv.mait.domain.user.service.component;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
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
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TeamRoleValidator {

	private final UserReader userReader;

	private final TeamReader teamReader;

	private final TeamUserEntityRepository teamUserEntityRepository;

	private final SolvingSessionEntityRepository solvingSessionEntityRepository;

	public boolean canViewQuestionAnswers(final QuestionSetEntity questionSet, final Long userId) {
		TeamUserRole role = getTeamRole(questionSet.getTeamId(), userId);
		if (role.covers(TeamUserRole.MAKER)) {
			return true;
		}
		if (questionSet.getStatus() == QuestionSetStatus.MAKING) {
			throw new UserRoleException("제작 중인 문제는 관리자만 조회할 수 있습니다.");
		}
		if (questionSet.canReview()) {
			return true;
		}
		return questionSet.getSolveMode() == QuestionSetSolveMode.STUDY
			&& solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndSolveMode(userId,
				questionSet.getId(), QuestionSetSolveMode.STUDY)
			.map(session -> session.getStatus() == SolvingStatus.COMPLETE)
			.orElse(false);
	}

	public TeamUserRole getTeamRole(final Long teamId, final Long userId) {
		TeamEntity team = teamReader.getActiveTeam(teamId);
		UserEntity user = userReader.getById(userId);
		return teamUserEntityRepository.findByTeamAndUser(team, user)
			.orElseThrow(() -> new UserRoleException("해당 팀의 멤버가 아닙니다."))
			.getUserRole();
	}

	public void checkIsTeamManager(final Long teamId, final Long userId) {
		if (!getTeamRole(teamId, userId).covers(TeamUserRole.MAKER)) {
			throw new UserRoleException("팀 관리 권한이 없습니다.");
		}
	}

	public void checkHasSolveQuestionAuthorityInTeam(final Long teamId, final Long userId) {
		TeamEntity team = teamReader.getActiveTeam(teamId);

		UserEntity user = userReader.getById(userId);

		if (!teamUserEntityRepository.existsByTeamAndUser(team, user)) {
			throw new UserRoleException("해당 문제를 풀 수 있는 권한이 없습니다.");
		}
	}

	public void checkIsTeamMember(final Long teamId, final Long userId) {
		TeamEntity team = teamReader.getActiveTeam(teamId);

		UserEntity user = userReader.getById(userId);

		if (!teamUserEntityRepository.existsByTeamAndUser(team, user)) {
			throw new UserRoleException("해당 팀의 멤버가 아닙니다.");
		}
	}

	public void checkIsTeamOwner(final Long teamId, final Long userId) {
		TeamEntity team = teamReader.getActiveTeam(teamId);

		UserEntity user = userReader.getById(userId);

		TeamUserEntity teamUser = teamUserEntityRepository.findByTeamAndUser(team, user)
			.orElseThrow(() -> new EntityNotFoundException("해당 팀의 멤버가 아닙니다."));

		if (teamUser.getUserRole() != TeamUserRole.OWNER) {
			throw new UserRoleException("해당 팀의 OWNER가 아닙니다.");
		}
	}
}
