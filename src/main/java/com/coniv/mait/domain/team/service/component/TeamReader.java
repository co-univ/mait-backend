package com.coniv.mait.domain.team.service.component;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.exception.TeamManagerException;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TeamReader {

	private final TeamEntityRepository teamEntityRepository;

	public TeamEntity getTeam(final Long teamId) {
		return teamEntityRepository.findById(teamId)
			.orElseThrow(() -> new EntityNotFoundException(teamId + " : 해당 팀을 찾을 수 없습니다."));
	}

	public TeamEntity getActiveTeam(final Long teamId) {
		TeamEntity team = teamEntityRepository.findById(teamId)
			.orElseThrow(() -> new EntityNotFoundException(teamId + " : 해당 팀을 찾을 수 없습니다."));
		validateActiveTeam(team);
		return team;
	}

	public void validateActiveTeam(final TeamEntity team) {
		if (team.deleted()) {
			throw new TeamManagerException("삭제된 팀입니다.");
		}
	}
}
