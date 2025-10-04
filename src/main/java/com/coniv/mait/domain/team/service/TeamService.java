package com.coniv.mait.domain.team.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInviteEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInviteEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.component.InviteTokenGenerator;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.enums.InviteTokenDuration;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

	private final TeamEntityRepository teamEntityRepository;
	private final TeamUserEntityRepository teamUserEntityRepository;
	private final InviteTokenGenerator inviteTokenGenerator;
	private final TeamInviteEntityRepository teamInviteEntityRepository;

	@Transactional
	public void createTeam(final String teamName, final UserEntity owner) {
		TeamEntity teamEntity = teamEntityRepository.save(TeamEntity.of(teamName));
		teamUserEntityRepository.save(TeamUserEntity.createOwnerUser(owner, teamEntity));
	}

	@Transactional
	public void createUsersAndLinkTeam(final List<UserEntity> users, final TeamEntity team) {
		List<TeamUserEntity> teamUsers = users.stream()
			.map(user -> TeamUserEntity.createPlayerUser(user, team))
			.toList();
		teamUserEntityRepository.saveAll(teamUsers);
	}

	@Transactional
	public String createTeamInviteCode(final Long teamId, final UserEntity inviter,
		final InviteTokenDuration duration) {
		TeamEntity team = teamEntityRepository.findById(teamId)
			.orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));

		validateInviterRole(team, inviter);
		String privateCode = inviteTokenGenerator.generateUniqueInviteToken();

		TeamInviteEntity teamInviteEntity = TeamInviteEntity.createInvite(inviter, team, privateCode, duration);
		teamInviteEntityRepository.save(teamInviteEntity);

		return privateCode;
	}

	private void validateInviterRole(final TeamEntity team, final UserEntity inviter) {
		TeamUserEntity teamUser = teamUserEntityRepository.findByTeamIdAndUserId(team, inviter)
			.orElseThrow(() -> new EntityNotFoundException("Inviter is not a member of the team"));

		if (!teamUser.canInvite()) {
			throw new IllegalArgumentException("Only team owners can create invite codes");
		}
	}
}
