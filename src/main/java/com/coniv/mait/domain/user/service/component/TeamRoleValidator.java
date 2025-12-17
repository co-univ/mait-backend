package com.coniv.mait.domain.user.service.component;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.repository.UserEntityRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TeamRoleValidator {

	private final UserEntityRepository userEntityRepository;

	private final TeamEntityRepository teamEntityRepository;

	private final TeamUserEntityRepository teamUserEntityRepository;

	public void checkHasCreateQuestionSetAuthority(final Long teamId, final Long userId) {
		TeamEntity team = teamEntityRepository.findById(teamId)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 팀입니다."));

		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자입니다."));

		TeamUserEntity teamUser = teamUserEntityRepository.findByTeamAndUser(team, user)
			.orElseThrow(() -> new EntityNotFoundException("해당 팀의 멤버가 아닙니다."));

		if (!teamUser.getUserRole().canCreateQuestionSet()) {
			throw new UserRoleException("문제 세트 생성 권한이 없습니다.");
		}
	}

	public void checkHasSolveQuestionAuthorityInTeam(final Long teamId, final Long userId) {
		TeamEntity team = teamEntityRepository.findById(teamId)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 팀입니다."));

		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자입니다."));

		if (!teamUserEntityRepository.existsByTeamAndUser(team, user)) {
			throw new UserRoleException("해당 문제를 풀 수 있는 권한이 없습니다.");
		}
	}
}
