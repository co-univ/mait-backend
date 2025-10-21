package com.coniv.mait.domain.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.team.entity.TeamInviteEntity;

public interface TeamInviteEntityRepository extends JpaRepository<TeamInviteEntity, Long> {
	boolean existsByToken(String token);
}
