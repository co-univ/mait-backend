package com.coniv.mait.domain.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.team.entity.TeamUserEntity;

public interface TeamUserEntityRepository extends JpaRepository<TeamUserEntity, Long> {
}
