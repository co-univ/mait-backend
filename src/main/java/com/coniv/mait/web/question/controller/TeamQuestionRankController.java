package com.coniv.mait.web.question.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.TeamQuestionRankService;
import com.coniv.mait.domain.solve.service.dto.RankDto;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.TeamRankApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "팀 문제 랭킹 API", description = "팀 문제 랭킹 API")
@RestController
@RequestMapping("/api/v1/teams/{teamId}/question-ranks")
@RequiredArgsConstructor
public class TeamQuestionRankController {

	private final TeamQuestionRankService teamQuestionRankService;

	@Operation(summary = "팀 정답 퀴즈 랭킹 조회", description = "완료된 퀴즈에서 정답자 랭킹 반환",
		parameters = @Parameter(name = "type", description = "랭킹 타입", required = true, example = "CORRECT"))
	@GetMapping(params = "type=CORRECT")
	public ResponseEntity<ApiResponse<TeamRankApiResponse>> getTeamQuestionCorrectRank(
		@PathVariable Long teamId,
		@Parameter(description = "노출할 랭크 개수")
		@RequestParam(value = "rankCount", required = false, defaultValue = "3") int rankCount,
		@AuthenticationPrincipal MaitUser user) {
		List<RankDto> ranks = teamQuestionRankService.getTeamQuestionCorrectAnswerRank(teamId);
		return ResponseEntity.ok(
			ApiResponse.ok(
				TeamRankApiResponse.of(ranks, teamId, user.id(), rankCount, TeamRankApiResponse.RankType.CORRECT)));
	}

	@Operation(summary = "팀 득점자 퀴즈 랭킹 조회", description = "완료된 퀴즈에서 득점자 랭킹 반환")
	@GetMapping(params = "type=SCORER")
	public ResponseEntity<ApiResponse<TeamRankApiResponse>> getTeamQuestionScorerRank(
		@PathVariable Long teamId,
		@Parameter(description = "노출할 랭크 개수")
		@RequestParam(value = "rankCount", required = false, defaultValue = "3") int rankCount,
		@AuthenticationPrincipal MaitUser user) {
		List<RankDto> ranks = teamQuestionRankService.getTeamQuestionScorerRank(teamId);
		return ResponseEntity.ok(
			ApiResponse.ok(
				TeamRankApiResponse.of(ranks, teamId, user.id(), rankCount, TeamRankApiResponse.RankType.SCORER)));
	}
}
