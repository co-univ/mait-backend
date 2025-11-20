package com.coniv.mait.web.team.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.team.service.dto.TeamApplicantDto;
import com.coniv.mait.domain.team.service.dto.TeamDto;
import com.coniv.mait.domain.team.service.dto.TeamInvitationDto;
import com.coniv.mait.domain.team.service.dto.TeamInvitationLinkDto;
import com.coniv.mait.domain.team.service.dto.TeamInvitationResultDto;
import com.coniv.mait.domain.team.service.dto.TeamUserDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.team.dto.AddTeamUserApiRequest;
import com.coniv.mait.web.team.dto.ApplyTeamUserApiResponse;
import com.coniv.mait.web.team.dto.ApproveTeamApplicationApiRequest;
import com.coniv.mait.web.team.dto.CreateTeamApiRequest;
import com.coniv.mait.web.team.dto.CreateTeamInviteApiRequest;
import com.coniv.mait.web.team.dto.CreateTeamInviteApiResponse;
import com.coniv.mait.web.team.dto.JoinedTeamUserApiResponse;
import com.coniv.mait.web.team.dto.TeamApiResponse;
import com.coniv.mait.web.team.dto.TeamInvitationApplyApiResponse;
import com.coniv.mait.web.team.dto.TeamInvitationLinksApiResponse;
import com.coniv.mait.web.team.dto.TeamInviteApiResponse;
import com.coniv.mait.web.team.dto.UpdateTeamUserRoleApiRequest;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

	private final TeamService teamService;

	@Operation(summary = "팀 생성 API")
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> createTeam(@AuthenticationPrincipal UserEntity ownerUser,
		@Valid @RequestBody CreateTeamApiRequest request) {
		teamService.createTeam(request.name(), ownerUser);
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "팀 초대 링크 생성 API")
	@PostMapping("/{teamId}/invitation")
	public ResponseEntity<ApiResponse<CreateTeamInviteApiResponse>> createTeamInviteCode(
		@PathVariable("teamId") Long teamId,
		@RequestBody @Valid CreateTeamInviteApiRequest request,
		@AuthenticationPrincipal UserEntity ownerUser,
		@RequestParam(defaultValue = "false") boolean requiresApproval) {
		String inviteCode = teamService.createTeamInviteCode(teamId, ownerUser, request.duration(), request.role(),
			requiresApproval);
		return ResponseEntity.ok()
			.body(ApiResponse.ok(
				CreateTeamInviteApiResponse.from(inviteCode)
			));
	}

	@Operation(summary = "초대 링크 팀 참가 API")
	@PostMapping("/{teamId}/applicant")
	public ResponseEntity<ApiResponse<TeamInvitationApplyApiResponse>> applyTeamInvitation(
		@PathVariable("teamId") Long teamId,
		@RequestParam("code") String code,
		@AuthenticationPrincipal UserEntity userPrincipal) {
		TeamInvitationResultDto result = teamService.applyTeamInvitation(teamId, code, userPrincipal);
		return ResponseEntity.ok(ApiResponse.ok(
			TeamInvitationApplyApiResponse.of(teamId, result)
		));
	}

	@Operation(summary = "초대 링크 신청 허용 API")
	@PostMapping("/{teamId}/applicant/{applicationId}")
	public ResponseEntity<ApiResponse<Void>> approveTeamApplication(
		@PathVariable("teamId") Long teamId,
		@PathVariable("applicationId") Long applicationId,
		@Valid @RequestBody ApproveTeamApplicationApiRequest request,
		@AuthenticationPrincipal UserEntity userPrincipal
	) {
		teamService.approveTeamApplication(teamId, applicationId, request.status(), userPrincipal);
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "초대 링크 팀 정보 반환 API")
	@GetMapping("/invitation/info")
	public ResponseEntity<ApiResponse<TeamInviteApiResponse>> getTeamInfo(
		@AuthenticationPrincipal UserEntity userPrincipal, @RequestParam("code") String code) {
		TeamInvitationDto teamInviteInfo = teamService.getTeamInviteInfo(userPrincipal, code);
		return ResponseEntity.ok(ApiResponse.ok(TeamInviteApiResponse.from(teamInviteInfo)));
	}

	@Operation(summary = "초대 링크 목록 반환 API")
	@GetMapping("/{teamId}/invitations")
	public ResponseEntity<ApiResponse<List<TeamInvitationLinksApiResponse>>> getTeamInvitations(
		@PathVariable("teamId") Long teamId) {
		List<TeamInvitationLinkDto> invitations = teamService.getTeamInvitations(teamId);
		List<TeamInvitationLinksApiResponse> response = invitations.stream()
			.map(TeamInvitationLinksApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@Operation(summary = "초대 링크 삭제 API")
	@DeleteMapping("/invitations/{invitationId}")
	public ResponseEntity<ApiResponse<Void>> deleteTeamInvitation(
		@PathVariable("invitationId") Long invitationId) {
		teamService.deleteTeamInvitation(invitationId);
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "가입 팀 목록 반환 API")
	@GetMapping("/joined")
	public ResponseEntity<ApiResponse<List<TeamApiResponse>>> getJoinedTeams(
		@AuthenticationPrincipal UserEntity userPrincipal) {
		List<TeamDto> joinedTeams = teamService.getJoinedTeams(userPrincipal);
		List<TeamApiResponse> response = joinedTeams.stream()
			.map(TeamApiResponse::of)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@Operation(summary = "팀 가입 회원 목록 반환 API")
	@GetMapping("/{teamId}/users")
	public ResponseEntity<ApiResponse<List<JoinedTeamUserApiResponse>>> getTeamUsers(
		@PathVariable("teamId") Long teamId) {
		List<TeamUserDto> teamUsers = teamService.getTeamUsers(teamId);
		return ResponseEntity.ok(ApiResponse.ok(
			teamUsers.stream()
				.map(JoinedTeamUserApiResponse::from)
				.toList()
		));
	}

	@Operation(summary = "팀 가입 승인 대기 회원 목록 반환 API")
	@GetMapping("/{teamId}/applicants")
	public ResponseEntity<ApiResponse<List<ApplyTeamUserApiResponse>>> getTeamApplicants(
		@PathVariable("teamId") Long teamId) {
		List<TeamApplicantDto> applicants = teamService.getApplicants(teamId);
		return ResponseEntity.ok(ApiResponse.ok(
			applicants.stream()
				.map(ApplyTeamUserApiResponse::from)
				.toList()
		));
	}

	@Operation(summary = "팀 유저 삭제 API")
	@DeleteMapping("/team-users/{teamUserId}")
	public ResponseEntity<ApiResponse<Void>> deleteTeamUser(@PathVariable("teamUserId") Long teamUserId) {
		teamService.deleteTeamUser(teamUserId);
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "팀 유저 역할 변경 API")
	@PatchMapping("/team-users/{teamUserId}/role")
	public ResponseEntity<ApiResponse<Void>> updateTeamUserRole(
		@PathVariable("teamUserId") Long teamUserId,
		@Valid @RequestBody UpdateTeamUserRoleApiRequest request) {
		teamService.updateTeamUserRole(teamUserId, request.role());
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "팀에 유저 추가 API")
	@PostMapping("/{teamId}/team-users")
	public ResponseEntity<ApiResponse<Void>> addUserInTeam(
		@PathVariable("teamId") Long teamId,
		@Valid @RequestBody AddTeamUserApiRequest request) {
		teamService.addUserInTeam(teamId, request.userId(), request.role());
		return ResponseEntity.ok(ApiResponse.noContent());
	}
}
