package com.coniv.mait.domain.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.team.entity.TeamInviteApplicantEntity;

public interface TeamInviteApplicationEntityRepository extends JpaRepository<TeamInviteApplicantEntity, Long> {
}
