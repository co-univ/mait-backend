package com.coniv.mait.domain.team.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamInvitationApplicantEntity;
import com.coniv.mait.domain.team.entity.TeamInvitationLinkEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.InvitationApplicationStatus;
import com.coniv.mait.domain.team.enums.JoinedImmediate;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.event.MemberEmailInfo;
import com.coniv.mait.domain.team.event.TeamDeletedEvent;
import com.coniv.mait.domain.team.event.TeamMemberLeftEvent;
import com.coniv.mait.domain.team.exception.InvitationErrorCode;
import com.coniv.mait.domain.team.exception.TeamInvitationFailException;
import com.coniv.mait.domain.team.exception.TeamManagerException;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInvitationApplicationEntityRepository;
import com.coniv.mait.domain.team.repository.TeamInvitationEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.component.InviteTokenGenerator;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.team.service.dto.TeamApplicantDto;
import com.coniv.mait.domain.team.service.dto.TeamInvitationDto;
import com.coniv.mait.domain.team.service.dto.TeamInvitationLinkDto;
import com.coniv.mait.domain.team.service.dto.TeamInvitationResultDto;
import com.coniv.mait.domain.team.service.dto.TeamUserDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.enums.InviteTokenDuration;
import com.coniv.mait.global.event.MaitEventPublisher;

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
	private final MaitEventPublisher maitEventPublisher;
	private final TeamReader teamReader;
	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final TeamRoleValidator teamRoleValidator;

	@Transactional
	public void createTeam(final String teamName, final Long ownerId) {
		UserEntity owner = userEntityRepository.findById(ownerId)
			.orElseThrow(() -> new EntityNotFoundException("Owner user not found with id: " + ownerId));
		TeamEntity teamEntity = teamEntityRepository.save(TeamEntity.of(teamName, owner.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createOwnerUser(owner, teamEntity));
	}

	@Transactional(readOnly = true)
	public TeamInvitationDto getTeamInviteInfo(final MaitUser userPrincipal, final String invitationToken) {
		LocalDateTime applicationTime = LocalDateTime.now();
		TeamInvitationLinkEntity teamInvitationLink = teamInvitationEntityRepository.findByTokenFetchJoinTeam(
				invitationToken)
			.orElseThrow(() -> new TeamInvitationFailException(InvitationErrorCode.NOT_FOUND_CODE));
		TeamEntity team = teamInvitationLink.getTeam();
		teamReader.validateActiveTeam(team);

		if (teamInvitationLink.isExpired(applicationTime)) {
			throw new TeamInvitationFailException(InvitationErrorCode.EXPIRED_CODE);
		}

		if (userPrincipal == null) {
			return TeamInvitationDto.from(teamInvitationLink, team, null);
		}

		UserEntity user = userEntityRepository.findById(userPrincipal.id())
			.orElseThrow(() -> new EntityNotFoundException(
				"User not found with id: " + userPrincipal.id()));

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
	public String createTeamInviteCode(final Long teamId, final Long invitorId,
		final InviteTokenDuration duration, final TeamUserRole role, final boolean requiresApproval) {
		if (role == TeamUserRole.OWNER) {
			throw new TeamInvitationFailException(InvitationErrorCode.CANNOT_CREATE_WITH_OWNER_ROLE);
		}
		TeamEntity team = teamReader.getActiveTeam(teamId);
		UserEntity invitor = userEntityRepository.findById(invitorId)
			.orElseThrow(
				() -> new EntityNotFoundException("Owner user not found with id: " + invitorId));

		validateInvitorRole(team, invitor);
		String privateCode = inviteTokenGenerator.generateUniqueInviteToken();

		TeamInvitationLinkEntity teamInvitationLinkEntity = TeamInvitationLinkEntity.createInvite(invitor, team,
			privateCode, duration,
			role, requiresApproval);
		teamInvitationEntityRepository.save(teamInvitationLinkEntity);

		return privateCode;
	}

	@Transactional
	public void addUserInTeam(final Long teamId, final Long userId, final TeamUserRole role) {
		if (role == TeamUserRole.OWNER) {
			throw new TeamManagerException("Cannot add team user with OWNER role.");
		}
		TeamEntity team = teamReader.getActiveTeam(teamId);
		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

		if (isUserInTeam(team, user)) {
			throw new TeamManagerException("User is already a member of the team.");
		}

		TeamUserEntity teamUser = TeamUserEntity.createTeamUser(user, team, role);
		teamUserEntityRepository.save(teamUser);
	}

	@Transactional
	public void approveTeamApplication(Long teamId, Long applicationId, final InvitationApplicationStatus newStatus,
		Long approverId) {
		if (newStatus == InvitationApplicationStatus.PENDING) {
			throw new TeamInvitationFailException(InvitationErrorCode.CANNOT_SET_TO_PENDING);
		}

		TeamEntity team = teamReader.getActiveTeam(teamId);

		validateApplicationApprover(team, approverId);

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

	private void validateApplicationApprover(TeamEntity team, Long approverId) {
		UserEntity approver = userEntityRepository.findById(approverId)
			.orElseThrow(
				() -> new EntityNotFoundException("Approver user not found with id: " + approverId));

		TeamUserEntity approverTeamUser = teamUserEntityRepository.findByTeamAndUser(team, approver)
			.orElseThrow(() -> new EntityNotFoundException(
				"Approver is not a member of the team " + team.getId() + ", user: " + approver.getId()));

		if (!approverTeamUser.canApproveApplications()) {
			throw new TeamInvitationFailException(InvitationErrorCode.ONLY_OWNER_OR_MAKER_APPROVE);
		}
	}

	@Transactional
	public TeamInvitationResultDto applyTeamInvitation(final Long teamId, final String code,
		final Long userId) {
		LocalDateTime applyTime = LocalDateTime.now();

		TeamInvitationLinkEntity invitationLink = teamInvitationEntityRepository.findByTokenFetchJoinTeam(code)
			.orElseThrow(() -> new TeamInvitationFailException(InvitationErrorCode.NOT_FOUND_CODE));
		TeamEntity team = invitationLink.getTeam();
		teamReader.validateActiveTeam(team);
		if (!teamId.equals(team.getId())) {
			throw new TeamInvitationFailException(InvitationErrorCode.TOKEN_NOT_BELONG_TEAM);
		}

		if (invitationLink.isExpired(applyTime)) {
			throw new TeamInvitationFailException(InvitationErrorCode.EXPIRED_CODE);
		}

		UserEntity applicant = userEntityRepository.findById(userId)
			.orElseThrow(
				() -> new EntityNotFoundException("Applicant user not found with id: " + userId));

		if (isUserInTeam(team, applicant)) {
			throw new TeamInvitationFailException(InvitationErrorCode.ALREADY_MEMBER);
		}

		if (teamInvitationApplicationEntityRepository.existsByTeamIdAndUserIdAndApplicationStatus(
			team.getId(), applicant.getId(), InvitationApplicationStatus.PENDING)) {
			throw new TeamInvitationFailException(InvitationErrorCode.USER_ALREADY_APPLIED);
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
			return TeamInvitationResultDto.from(JoinedImmediate.APPROVAL_REQUIRED);
		}

		TeamUserEntity teamUser = TeamUserEntity.createTeamUser(applicant, team, invitationLink.getRoleOnJoin());
		teamUserEntityRepository.save(teamUser);
		return TeamInvitationResultDto.from(JoinedImmediate.IMMEDIATE);
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

	@Transactional(readOnly = true)
	public List<TeamUserDto> getJoinedTeams(final Long userId) {
		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

		return teamUserEntityRepository.findAllByUserFetchJoinActiveTeam(user).stream()
			.map(TeamUserDto::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<TeamUserDto> getTeamUsers(final Long teamId) {
		teamReader.getActiveTeam(teamId);
		List<TeamUserEntity> teamUsers = teamUserEntityRepository.findAllByTeamIdFetchJoinUser(teamId);

		return teamUsers.stream()
			.map(TeamUserDto::from)
			.sorted(Comparator.comparing(TeamUserDto::getUserName))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<TeamApplicantDto> getApplicants(Long teamId) {
		teamReader.getActiveTeam(teamId);
		List<TeamInvitationApplicantEntity> pendingApplicants = teamInvitationApplicationEntityRepository
			.findAllByTeamIdAndApplicationStatus(teamId, InvitationApplicationStatus.PENDING);

		Map<Long, TeamInvitationApplicantEntity> byUserId = pendingApplicants.stream()
			.collect(Collectors.toMap(
				TeamInvitationApplicantEntity::getUserId,
				applicant -> applicant,
				(existing, replacement) -> existing
			));

		List<Long> userIds = pendingApplicants.stream()
			.map(TeamInvitationApplicantEntity::getUserId)
			.toList();
		List<UserEntity> users = userEntityRepository.findAllById(userIds);

		return users.stream()
			.map(user -> {
				TeamInvitationApplicantEntity applicant = byUserId.get(user.getId());
				return TeamApplicantDto.of(applicant, user);
			})
			.sorted(Comparator.comparing(TeamApplicantDto::getName))
			.toList();
	}

	@Transactional
	public void deleteTeam(final Long teamId, final Long userId) {
		TeamEntity team = teamReader.getActiveTeam(teamId);
		teamRoleValidator.checkIsTeamOwner(teamId, userId);

		List<TeamUserEntity> teamUsers = teamUserEntityRepository.findAllByTeamIdFetchJoinUser(teamId);
		List<MemberEmailInfo> recipients = teamUsers.stream()
			.map(teamUser -> new MemberEmailInfo(teamUser.getUser().getName(), teamUser.getUser().getEmail()))
			.toList();

		List<QuestionSetEntity> ongoingLiveQuestionSets =
			questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(teamId, QuestionSetSolveMode.LIVE_TIME,
				List.of(QuestionSetStatus.ONGOING)
			);
		ongoingLiveQuestionSets.forEach(QuestionSetEntity::endLiveQuestionSet);
		List<Long> ongoingLiveQuestionSetIds = ongoingLiveQuestionSets.stream()
			.map(QuestionSetEntity::getId)
			.toList();

		team.markDeleted();
		maitEventPublisher.publishEvent(TeamDeletedEvent.builder()
			.teamId(teamId)
			.teamName(team.getName())
			.recipients(recipients)
			.ongoingLiveQuestionSetIds(ongoingLiveQuestionSetIds)
			.build());
	}

	@Transactional
	public void deleteTeamUser(Long teamUserId) {
		TeamUserEntity teamUser = teamUserEntityRepository.findById(teamUserId)
			.orElseThrow(() -> new EntityNotFoundException("Team user not found with id: " + teamUserId));
		teamReader.validateActiveTeam(teamUser.getTeam());

		teamUserEntityRepository.delete(teamUser);
	}

	@Transactional
	public void leaveTeam(final Long teamId, final Long userId) {
		TeamEntity team = teamReader.getActiveTeam(teamId);
		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

		TeamUserEntity teamUser = teamUserEntityRepository.findByTeamAndUser(team, user)
			.orElseThrow(() -> new EntityNotFoundException(
				"Team user not found with teamId: " + teamId + ", userId: " + userId));

		if (teamUser.getUserRole() == TeamUserRole.OWNER) {
			throw new TeamManagerException("Owner cannot leave the team.");
		}

		teamUserEntityRepository.delete(teamUser);
		TeamUserEntity owner = teamUserEntityRepository.findByTeamIdAndUserRole(teamId, TeamUserRole.OWNER)
			.orElseThrow(() -> new EntityNotFoundException("owner가 존재하지 않습니다."));

		maitEventPublisher.publishEvent(TeamMemberLeftEvent.builder()
			.memberName(user.getName())
			.teamName(team.getName())
			.memberEmail(user.getEmail())
			.ownerEmail(owner.getUser().getEmail())
			.build());
	}

	@Transactional
	public void updateTeamUserRole(Long teamUserId, TeamUserRole role) {
		if (role == TeamUserRole.OWNER) {
			throw new TeamManagerException("Cannot set team user role to OWNER.");
		}
		TeamUserEntity teamUser = teamUserEntityRepository.findById(teamUserId)
			.orElseThrow(() -> new EntityNotFoundException("Team user not found with id: " + teamUserId));
		teamReader.validateActiveTeam(teamUser.getTeam());

		if (teamUser.getUserRole() == TeamUserRole.OWNER) {
			throw new TeamManagerException("Cannot change role of OWNER.");
		}

		teamUser.updateUserRole(role);
	}

	@Transactional(readOnly = true)
	public List<TeamInvitationLinkDto> getTeamInvitations(Long teamId) {
		TeamEntity team = teamReader.getActiveTeam(teamId);

		return teamInvitationEntityRepository.findActiveLinksByTeam(team, LocalDateTime.now()).stream()
			.map(TeamInvitationLinkDto::from)
			.sorted(Comparator.comparing(TeamInvitationLinkDto::getExpiredAt))
			.toList();
	}

	@Transactional
	public void deleteTeamInvitation(Long invitationId) {
		TeamInvitationLinkEntity invitationLink = teamInvitationEntityRepository.findById(invitationId)
			.orElseThrow(() -> new EntityNotFoundException("Team invitation not found with id: " + invitationId));
		teamReader.validateActiveTeam(invitationLink.getTeam());

		invitationLink.changeToExpired();
	}
}
