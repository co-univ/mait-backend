package com.coniv.mait.domain.user.service.component;

import org.springframework.stereotype.Component;

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

	public void checkIsTeamManager(final Long teamId, final Long userId) {
		TeamEntity team = teamReader.getActiveTeam(teamId);

		UserEntity user = userReader.getById(userId);

		TeamUserEntity teamUser = teamUserEntityRepository.findByTeamAndUser(team, user)
			.orElseThrow(() -> new UserRoleException("해당 팀의 멤버가 아닙니다."));

		if (!teamUser.getUserRole().covers(TeamUserRole.MAKER)) {
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
