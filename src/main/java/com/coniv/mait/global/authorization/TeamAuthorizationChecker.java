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

	public boolean hasRole(
		Long teamId,
		UserEntity loginUser,
		TeamUserRole roleLevel
	) {
		if (loginUser == null) {
			throw new TeamManagerException("User is not authenticated.");
		}

		List<TeamUserRole> allowedRoles = switch (roleLevel) {
			case MAKER -> List.of(
				TeamUserRole.OWNER,
				TeamUserRole.MAKER
			);
			case PLAYER -> List.of(
				TeamUserRole.OWNER,
				TeamUserRole.MAKER,
				TeamUserRole.PLAYER
			);
			case OWNER -> List.of(
				TeamUserRole.OWNER
			);
		};

		boolean result = teamUserRepository.existsByTeamIdAndUserIdAndUserRoleIn(
			teamId,
			loginUser.getId(),
			allowedRoles
		);

		if (!result) {
			throw new TeamManagerException(
				"User %d does not have required role (%s) in team %d"
					.formatted(loginUser.getId(), roleLevel, teamId)
			);
		}

		return true;
	}
}
