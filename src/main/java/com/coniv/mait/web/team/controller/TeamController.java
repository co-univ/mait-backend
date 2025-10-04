package com.coniv.mait.web.team.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.team.dto.CreateTeamApiRequest;
import com.coniv.mait.web.team.dto.CreateTeamInviteApiRequest;

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
	@PostMapping("/{teamId}/invite")
	public ResponseEntity<ApiResponse<String>> createTeamInviteCode(@PathVariable("teamId") Long teamId,
		@RequestBody @Valid CreateTeamInviteApiRequest request,
		@AuthenticationPrincipal UserEntity ownerUser) {
		String inviteCode = teamService.createTeamInviteCode(teamId, ownerUser, request.duration());
		return ResponseEntity.ok(ApiResponse.ok(inviteCode));
	}
}
