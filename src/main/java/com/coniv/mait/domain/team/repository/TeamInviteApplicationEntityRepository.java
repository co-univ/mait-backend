package com.coniv.mait.domain.team.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.team.entity.TeamInviteApplicantEntity;

public interface TeamInviteApplicationEntityRepository extends JpaRepository<TeamInviteApplicantEntity, Long> {

	Optional<TeamInviteApplicantEntity> findByTeamIdAndUserIdAndInviteId(Long teamId, Long userId, Long inviteId);
}
