package com.coniv.mait.domain.team.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.user.entity.UserEntity;

public interface TeamUserEntityRepository extends JpaRepository<TeamUserEntity, Long> {
	List<TeamUserEntity> findAllByTeamId(Long teamId);

	Optional<TeamUserEntity> findByTeamIdAndUserId(TeamEntity team, UserEntity user);
}
