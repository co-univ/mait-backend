package com.coniv.mait.web.question.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.TeamQuestionRankService;
import com.coniv.mait.domain.team.service.dto.TeamQuestionRankCombinedDto;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.team.dto.TeamQuestionRankCombinedApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "팀 문제 랭킹 API", description = "팀 문제 랭킹 API")
@RestController
@RequestMapping("/api/v1/teams/{teamId}/question-ranks")
@RequiredArgsConstructor
public class TeamQuestionRankController {

	private final TeamQuestionRankService teamQuestionRankService;

	@Operation(summary = "팀 퀴즈 랭킹 조회", description = "완료된 퀴즈에서 랭킹 반환")
	@GetMapping("/live-status")
	public ResponseEntity<ApiResponse<TeamQuestionRankCombinedApiResponse>> getTeamQuestionRank(
		@PathVariable("teamId") Long teamId) {
		TeamQuestionRankCombinedDto combined = teamQuestionRankService.getTeamQuestionRankCombined(teamId);
		return ResponseEntity.ok(ApiResponse.ok(TeamQuestionRankCombinedApiResponse.from(combined)));
	}
}
