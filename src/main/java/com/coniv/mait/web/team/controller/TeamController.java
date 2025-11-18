package com.coniv.mait.web.team.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.team.service.dto.TeamInvitationDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.team.dto.CreateTeamApiRequest;
import com.coniv.mait.web.team.dto.CreateTeamInviteApiRequest;
import com.coniv.mait.web.team.dto.CreateTeamInviteApiResponse;
import com.coniv.mait.web.team.dto.TeamInviteApiResponse;

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

	@Operation(summary = "초대 링크 팀 정보 반환 API")
	@GetMapping("/invitation/info")
	public ResponseEntity<ApiResponse<TeamInviteApiResponse>> getTeamInfo(
		@AuthenticationPrincipal UserEntity userPrincipal, @RequestParam("code") String code) {
		TeamInvitationDto teamInviteInfo = teamService.getTeamInviteInfo(userPrincipal, code);
		return ResponseEntity.ok(ApiResponse.ok(TeamInviteApiResponse.from(teamInviteInfo)));
	}
}
