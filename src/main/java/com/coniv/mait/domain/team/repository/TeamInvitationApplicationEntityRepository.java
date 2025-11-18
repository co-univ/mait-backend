package com.coniv.mait.domain.team.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.team.entity.TeamInvitationApplicantEntity;

public interface TeamInvitationApplicationEntityRepository extends JpaRepository<TeamInvitationApplicantEntity, Long> {

	Optional<TeamInvitationApplicantEntity> findByTeamIdAndUserIdAndInvitationLinkId(Long teamId, Long userId,
		Long invitationLinkId);
}
