package com.coniv.mait.domain.team.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInvitationApplicantEntity;
import com.coniv.mait.domain.team.entity.TeamInvitationLinkEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.InvitationApplicationStatus;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.exception.InvitationErrorCode;
import com.coniv.mait.domain.team.exception.TeamInvitationFailException;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInvitationApplicationEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInvitationEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.component.InviteTokenGenerator;
import com.coniv.mait.domain.team.service.dto.TeamInvitationDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.enums.InviteTokenDuration;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

	private final TeamEntityRepository teamEntityRepository;
	private final TeamUserEntityRepository teamUserEntityRepository;
	private final InviteTokenGenerator inviteTokenGenerator;
	private final TeamInvitationEntityRepository teamInvitationEntityRepository;
	private final UserEntityRepository userEntityRepository;
	private final TeamInvitationApplicationEntityRepository teamInvitationApplicationEntityRepository;

	@Transactional
	public void createTeam(final String teamName, final UserEntity ownerPrincipal) {
		UserEntity owner = userEntityRepository.findById(ownerPrincipal.getId())
			.orElseThrow(() -> new EntityNotFoundException("Owner user not found with id: " + ownerPrincipal.getId()));
		TeamEntity teamEntity = teamEntityRepository.save(TeamEntity.of(teamName, owner.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createOwnerUser(owner, teamEntity));
	}

	@Transactional(readOnly = true)
	public TeamInvitationDto getTeamInviteInfo(final UserEntity userPrincipal, final String invitationToken) {
		LocalDateTime applicationTime = LocalDateTime.now();
		TeamInvitationLinkEntity teamInvitationLink = teamInvitationEntityRepository.findByTokenFetchJoinTeam(
				invitationToken)
			.orElseThrow(() -> new TeamInvitationFailException(InvitationErrorCode.NOT_FOUND_CODE));
		TeamEntity team = teamInvitationLink.getTeam();

		if (teamInvitationLink.isExpired(applicationTime)) {
			throw new TeamInvitationFailException(InvitationErrorCode.EXPIRED_CODE);
		}

		if (userPrincipal == null) {
			return TeamInvitationDto.from(teamInvitationLink, team, null);
		}

		UserEntity user = userEntityRepository.findById(userPrincipal.getId())
			.orElseThrow(() -> new EntityNotFoundException(
				"User not found with id: " + userPrincipal.getId()));

		if (isUserInTeam(team, user)) {
			throw new TeamInvitationFailException(InvitationErrorCode.ALREADY_MEMBER);
		}

		return teamInvitationApplicationEntityRepository.findByTeamIdAndUserIdAndInvitationLinkId(
				team.getId(),
				user.getId(),
				teamInvitationLink.getId()
			)
			.map(app -> TeamInvitationDto.from(teamInvitationLink, team, app.getApplicationStatus()))
			.orElseGet(() -> TeamInvitationDto.from(teamInvitationLink, team, null));
	}

	@Transactional
	public void createUsersAndLinkTeam(final List<UserEntity> users, final TeamEntity team) {
		List<TeamUserEntity> teamUsers = users.stream()
			.map(user -> TeamUserEntity.createPlayerUser(user, team))
			.toList();
		teamUserEntityRepository.saveAll(teamUsers);
	}

	@Transactional
	public String createTeamInviteCode(final Long teamId, final UserEntity invitorPrincipal,
		final InviteTokenDuration duration, final TeamUserRole role, final boolean requiresApproval) {
		if (role == TeamUserRole.OWNER) {
			throw new TeamInvitationFailException(InvitationErrorCode.CANNOT_CREATE_WITH_OWNER_ROLE);
		}
		TeamEntity team = teamEntityRepository.findById(teamId)
			.orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));
		UserEntity invitor = userEntityRepository.findById(invitorPrincipal.getId())
			.orElseThrow(
				() -> new EntityNotFoundException("Owner user not found with id: " + invitorPrincipal.getId()));

		validateInvitorRole(team, invitor);
		String privateCode = inviteTokenGenerator.generateUniqueInviteToken();

		TeamInvitationLinkEntity teamInvitationLinkEntity = TeamInvitationLinkEntity.createInvite(invitor, team,
			privateCode, duration,
			role, requiresApproval);
		teamInvitationEntityRepository.save(teamInvitationLinkEntity);

		return privateCode;
	}

	@Transactional
	public void approveTeamApplication(Long teamId, Long applicationId, final InvitationApplicationStatus newStatus,
		UserEntity approverPrincipal) {
		if (newStatus == InvitationApplicationStatus.PENDING) {
			throw new TeamInvitationFailException(InvitationErrorCode.CANNOT_SET_TO_PENDING);
		}

		TeamEntity team = teamEntityRepository.findById(teamId)
			.orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + teamId));

		validateApplicationApprover(team, approverPrincipal);

		TeamInvitationApplicantEntity application = teamInvitationApplicationEntityRepository.findById(applicationId)
			.orElseThrow(() -> new EntityNotFoundException("Application not found with id: " + applicationId));
		if (!application.getTeamId().equals(team.getId())) {
			throw new TeamInvitationFailException(InvitationErrorCode.APPLICATION_NOT_BELONG_TEAM);
		}

		if (application.getApplicationStatus() != InvitationApplicationStatus.PENDING) {
			throw new TeamInvitationFailException(InvitationErrorCode.APPLICATION_ALREADY_PROCESSED);
		}

		if (newStatus == InvitationApplicationStatus.REJECTED) {
			application.rejectApplication();
			return;
		}

		UserEntity applicant = userEntityRepository.findById(application.getUserId())
			.orElseThrow(
				() -> new EntityNotFoundException("Applicant user not found with id: " + application.getUserId()));
		if (isUserInTeam(team, applicant)) {
			throw new TeamInvitationFailException(InvitationErrorCode.ALREADY_MEMBER);
		}

		application.approveApplication();
		TeamUserEntity teamUser = TeamUserEntity.createTeamUser(applicant, team, application.getRole());
		teamUserEntityRepository.save(teamUser);
	}

	private void validateApplicationApprover(TeamEntity team, UserEntity approverPrincipal) {
		UserEntity approver = userEntityRepository.findById(approverPrincipal.getId())
			.orElseThrow(
				() -> new EntityNotFoundException("Approver user not found with id: " + approverPrincipal.getId()));

		TeamUserEntity approverTeamUser = teamUserEntityRepository.findByTeamAndUser(team, approver)
			.orElseThrow(() -> new EntityNotFoundException(
				"Approver is not a member of the team " + team.getId() + ", user: " + approver.getId()));

		if (!approverTeamUser.canApproveApplications()) {
			throw new TeamInvitationFailException(InvitationErrorCode.ONLY_OWNER_OR_MAKER_APPROVE);
		}
	}

	@Transactional
	public boolean applyTeamInvitation(final Long teamId, final String code, final UserEntity userPrincipal) {
		LocalDateTime applyTime = LocalDateTime.now();

		TeamInvitationLinkEntity invitationLink = teamInvitationEntityRepository.findByTokenFetchJoinTeam(code)
			.orElseThrow(() -> new TeamInvitationFailException(InvitationErrorCode.NOT_FOUND_CODE));
		TeamEntity team = invitationLink.getTeam();
		if (!teamId.equals(team.getId())) {
			throw new TeamInvitationFailException(InvitationErrorCode.TOKEN_NOT_BELONG_TEAM);
		}

		if (invitationLink.isExpired(applyTime)) {
			throw new TeamInvitationFailException(InvitationErrorCode.EXPIRED_CODE);
		}

		UserEntity applicant = userEntityRepository.findById(userPrincipal.getId())
			.orElseThrow(
				() -> new EntityNotFoundException("Applicant user not found with id: " + userPrincipal.getId()));

		if (isUserInTeam(team, applicant)) {
			throw new TeamInvitationFailException(InvitationErrorCode.ALREADY_MEMBER);
		}

		if (teamInvitationApplicationEntityRepository.existsByTeamIdAndUserIdAndInvitationLinkId(
			team.getId(), applicant.getId(), invitationLink.getId())) {
			throw new TeamInvitationFailException(InvitationErrorCode.USER_ALREADY_APPLIED);
		}

		if (invitationLink.isRequiresApproval()) {
			TeamInvitationApplicantEntity application = TeamInvitationApplicantEntity.createApplication(team.getId(),
				applicant.getId(), invitationLink.getId(), invitationLink.getRoleOnJoin(), applyTime
			);
			teamInvitationApplicationEntityRepository.save(application);
			return false;
		}

		TeamUserEntity teamUser = TeamUserEntity.createTeamUser(applicant, team, invitationLink.getRoleOnJoin());
		teamUserEntityRepository.save(teamUser);
		return true;
	}

	private void validateInvitorRole(final TeamEntity team, final UserEntity invitor) {
		TeamUserEntity teamUser = teamUserEntityRepository.findByTeamAndUser(team, invitor)
			.orElseThrow(() -> new EntityNotFoundException(
				"Invitor is not a member of the team " + team.getId() + ", user: " + invitor.getId()));

		if (!teamUser.canInvite()) {
			throw new TeamInvitationFailException(InvitationErrorCode.CANT_CREATE_INVITE);
		}
	}

	private boolean isUserInTeam(final TeamEntity team, final UserEntity user) {
		return teamUserEntityRepository.existsByTeamAndUser(team, user);
	}
}
