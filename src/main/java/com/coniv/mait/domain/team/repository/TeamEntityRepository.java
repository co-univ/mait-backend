package com.coniv.mait.domain.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.team.entity.TeamEntity;

public interface TeamEntityRepository extends JpaRepository<TeamEntity, Long> {
}
