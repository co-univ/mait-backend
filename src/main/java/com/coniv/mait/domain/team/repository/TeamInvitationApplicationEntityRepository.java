package com.coniv.mait.domain.team.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.team.entity.TeamInvitationApplicantEntity;
import com.coniv.mait.domain.team.enums.InvitationApplicationStatus;

public interface TeamInvitationApplicationEntityRepository extends JpaRepository<TeamInvitationApplicantEntity, Long> {

	Optional<TeamInvitationApplicantEntity> findByTeamIdAndUserIdAndInvitationLinkId(Long teamId, Long userId,
		Long invitationLinkId);

	boolean existsByTeamIdAndUserIdAndInvitationLinkId(Long teamId, Long userId, Long invitationLinkId);

	List<TeamInvitationApplicantEntity> findAllByTeamIdAndApplicationStatus(Long teamId,
		InvitationApplicationStatus status);

	boolean existsByTeamIdAndUserIdAndApplicationStatus(Long teamId, Long userId,
		InvitationApplicationStatus status);
}
