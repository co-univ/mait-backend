package com.coniv.mait.global.authorization;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.exception.TeamManagerException;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("teamAuth")
@RequiredArgsConstructor
public class TeamAuthorizationChecker {

	private final TeamUserEntityRepository teamUserRepository;

	public boolean isManager(Long teamId, UserEntity loginUser) {
		log.info("TeamAuth.isManager called - teamId: {}, userId: {}",
			teamId, loginUser != null ? loginUser.getId() : null);

		if (loginUser == null) {
			log.warn("TeamAuth.isManager - loginUser is null");
			throw new TeamManagerException("User is not authenticated.");
		}

		boolean result = teamUserRepository.existsByTeamIdAndUserIdAndUserRoleIn(
			teamId,
			loginUser.getId(),
			List.of(TeamUserRole.OWNER, TeamUserRole.MAKER)
		);

		log.info("TeamAuth.isManager result: {} for userId: {} in teamId: {}",
			result, loginUser.getId(), teamId);

		if (!result) {
			throw new TeamManagerException(
				String.format("User %d does not have manager permissions for team %d",
					loginUser.getId(), teamId));
		}

		return true;
	}

	public boolean isMember(Long teamId, UserEntity loginUser) {
		log.info("TeamAuth.isMember called - teamId: {}, userId: {}",
			teamId, loginUser != null ? loginUser.getId() : null);

		if (loginUser == null) {
			throw new TeamManagerException("User is not authenticated.");
		}

		boolean result = teamUserRepository.existsByTeamIdAndUserIdAndUserRoleIn(
			teamId,
			loginUser.getId(),
			List.of(TeamUserRole.OWNER, TeamUserRole.MAKER, TeamUserRole.PLAYER)
		);

		if (!result) {
			throw new TeamManagerException(
				String.format("User %d is not a member of team %d", loginUser.getId(), teamId));
		}

		return true;
	}

}
