package com.coniv.mait.domain.user.service.component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;

import lombok.RequiredArgsConstructor;

// Todo UserDomain에 대한 개념을 반환하는 형태로 개선 필요
@Component
@RequiredArgsConstructor
public class UserReader {

	private final TeamUserEntityRepository teamUserEntityRepository;
	private final UserEntityRepository userEntityRepository;

	public List<UserEntity> getUsersByTeam(final TeamEntity team) {
		return teamUserEntityRepository.findAllByTeamId(team.getId()).stream()
			.map(TeamUserEntity::getUser)
			.toList();
	}

	public Map<Long, UserEntity> getUserById(Collection<Long> userIds) {
		return userEntityRepository.findAllById(userIds).stream()
			.collect(Collectors.toUnmodifiableMap(UserEntity::getId, Function.identity()));
	}
}
