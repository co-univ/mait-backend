package com.coniv.mait.domain.user.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;

// Todo UserDomain에 대한 개념을 반환하는 형태로 개선 필요
@Component
@RequiredArgsConstructor
public class UserReader {

	private final TeamUserEntityRepository teamUserEntityRepository;

	public List<UserEntity> getUsersByTeam(final TeamEntity team) {
		return teamUserEntityRepository.findAllByTeamId(team.getId()).stream()
			.map(TeamUserEntity::getUser)
			.toList();
	}
}
