package com.coniv.mait.domain.team.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInviteEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInviteApplicationEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInviteEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.component.InviteTokenGenerator;
import com.coniv.mait.domain.team.service.dto.TeamInviteDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.enums.InviteTokenDuration;
import com.coniv.mait.global.exception.custom.TeamInviteFailException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

	private final TeamEntityRepository teamEntityRepository;
	private final TeamUserEntityRepository teamUserEntityRepository;
	private final InviteTokenGenerator inviteTokenGenerator;
	private final TeamInviteEntityRepository teamInviteEntityRepository;
	private final UserEntityRepository userEntityRepository;
	private final TeamInviteApplicationEntityRepository teamInviteApplicationEntityRepository;

	@Transactional
	public void createTeam(final String teamName, final UserEntity ownerPrincipal) {
		UserEntity owner = userEntityRepository.findById(ownerPrincipal.getId())
			.orElseThrow(() -> new EntityNotFoundException("Owner user not found with id: " + ownerPrincipal.getId()));
		TeamEntity teamEntity = teamEntityRepository.save(TeamEntity.of(teamName, owner.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createOwnerUser(owner, teamEntity));
	}

	@Transactional(readOnly = true)
	public TeamInviteDto getTeamInviteInfo(final UserEntity userPrincipal, final String inviteToken) {
		LocalDateTime applicationTime = LocalDateTime.now();
		TeamInviteEntity teamInvite = teamInviteEntityRepository.findByToken(inviteToken)
			.orElseThrow(() -> new TeamInviteFailException("Invite token not found: " + inviteToken));
		TeamEntity team = teamInvite.getTeam();

		if (teamInvite.isExpired(applicationTime)) {
			throw new TeamInviteFailException("Invite token has expired: " + inviteToken);
		}

		if (userPrincipal == null) {
			return TeamInviteDto.from(teamInvite, team, true, null);
		}

		UserEntity user = userEntityRepository.findById(userPrincipal.getId())
			.orElseThrow(() -> new EntityNotFoundException(
				"User not found with id: " + userPrincipal.getId()));

		System.out.println("User " + user.getId() + " is trying to join team " + team.getId());

		if (isUserInTeam(team, user)) {
			throw new TeamInviteFailException("User is already a member of the team: " + team.getId());
		}

		return teamInviteApplicationEntityRepository.findByTeamIdAndUserIdAndInviteId(
				team.getId(),
				user.getId(),
				teamInvite.getId()
			)
			.map(app -> TeamInviteDto.from(teamInvite, team, true, app.getApplicationStatus()))
			.orElse(TeamInviteDto.from(teamInvite, team, true, null));
	}

	@Transactional
	public void createUsersAndLinkTeam(final List<UserEntity> users, final TeamEntity team) {
		List<TeamUserEntity> teamUsers = users.stream()
			.map(user -> TeamUserEntity.createPlayerUser(user, team))
			.toList();
		teamUserEntityRepository.saveAll(teamUsers);
	}

	@Transactional
	public String createTeamInviteCode(final Long teamId, final UserEntity invitorPrincipal,
		final InviteTokenDuration duration, final TeamUserRole role, final boolean requiresApproval) {
		if (role == TeamUserRole.OWNER) {
			throw new TeamInviteFailException("Cannot create invite code with OWNER role");
		}
		TeamEntity team = teamEntityRepository.findById(teamId)
			.orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));
		UserEntity invitor = userEntityRepository.findById(invitorPrincipal.getId())
			.orElseThrow(
				() -> new EntityNotFoundException("Owner user not found with id: " + invitorPrincipal.getId()));

		validateInvitorRole(team, invitor);
		String privateCode = inviteTokenGenerator.generateUniqueInviteToken();

		TeamInviteEntity teamInviteEntity = TeamInviteEntity.createInvite(invitor, team, privateCode, duration,
			role, requiresApproval);
		teamInviteEntityRepository.save(teamInviteEntity);

		return privateCode;
	}

	private void validateInvitorRole(final TeamEntity team, final UserEntity invitor) {
		TeamUserEntity teamUser = teamUserEntityRepository.findByTeamAndUser(team, invitor)
			.orElseThrow(() -> new EntityNotFoundException(
				"Invitor is not a member of the team " + team.getId() + ", user: " + invitor.getId()));

		if (!teamUser.canInvite()) {
			throw new TeamInviteFailException("Only team owners can create invite codes");
		}
	}

	private boolean isUserInTeam(final TeamEntity team, final UserEntity user) {
		System.out.println("Checking if user " + user.getId() + " is in team " + team.getId());
		return teamUserEntityRepository.existsByTeamAndUser(team, user);
	}
}
