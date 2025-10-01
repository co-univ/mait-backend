package com.coniv.mait.domain.team.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

	private final TeamEntityRepository teamEntityRepository;
	private final TeamUserEntityRepository teamUserEntityRepository;

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
}
