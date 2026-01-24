package com.coniv.mait.web.question.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.TeamQuestionRankService;
import com.coniv.mait.domain.team.service.dto.TeamQuestionRankDto;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.team.dto.TeamQuestionRankApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "팀 문제 랭킹 API", description = "팀 문제 랭킹 API")
@RestController
@RequestMapping("/api/v1/teams/{teamId}/question-ranks")
@RequiredArgsConstructor
public class TeamQuestionRankController {

	private final TeamQuestionRankService teamQuestionRankService;

	@GetMapping("/live-status")
	public ResponseEntity<ApiResponse<List<TeamQuestionRankApiResponse>>> getTeamQuestionRank(
		@PathVariable("teamId") Long teamId) {
		List<TeamQuestionRankDto> rankList = teamQuestionRankService.getTeamQuestionRank(teamId);
		List<TeamQuestionRankApiResponse> response = rankList.stream()
			.map(TeamQuestionRankApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

}
