package com.coniv.mait.web.team.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.team.service.TeamService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.team.dto.CreateTeamApiRequest;

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
	public ResponseEntity<ApiResponse<Void>> createTeam(@Valid @RequestBody CreateTeamApiRequest request) {
		//TODO: @AuthenticationPrincipal UserEntity user 추가
		teamService.createTeam(request.name());
		return ResponseEntity.ok(ApiResponse.noContent());
	}
}
