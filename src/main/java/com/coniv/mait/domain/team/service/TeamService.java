package com.coniv.mait.domain.team.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

	private final TeamEntityRepository teamEntityRepository;

	@Transactional
	public void createTeam(final String teamName) {
		TeamEntity teamEntity = teamEntityRepository.save(TeamEntity.of(teamName));

		//TODO: 추후 UserEntity와 연동하여 TeamMemberEntity를 생성하는 로직 추가
	}
}
