package com.coniv.mait.web.question.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.TeamQuestionRankService;
import com.coniv.mait.domain.team.service.dto.QuestionRanksDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.TeamQuestionRankCombinedApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "팀 문제 랭킹 API", description = "팀 문제 랭킹 API")
@RestController
@RequestMapping("/api/v1/teams/{teamId}/question-ranks")
@RequiredArgsConstructor
public class TeamQuestionRankController {

	private final TeamQuestionRankService teamQuestionRankService;

	@Operation(summary = "팀 정답 퀴즈 랭킹 조회", description = "완료된 퀴즈에서 정답자 랭킹 반환")
	@GetMapping(params = "type=CORRECT")
	public ResponseEntity<ApiResponse<TeamQuestionRankCombinedApiResponse>> getTeamQuestionCorrectRank(
		@PathVariable("teamId") Long teamId,
		@AuthenticationPrincipal UserEntity user) {
		QuestionRanksDto combined =
			teamQuestionRankService.getTeamQuestionCorrectAnswerRank(teamId, user.getId());
		return ResponseEntity.ok(
			ApiResponse.ok(TeamQuestionRankCombinedApiResponse.from(combined)));
	}

	@Operation(summary = "팀 득점자 퀴즈 랭킹 조회", description = "완료된 퀴즈에서 득점자 랭킹 반환")
	@GetMapping(params = "type=SCORER")
	public ResponseEntity<ApiResponse<TeamQuestionRankCombinedApiResponse>> getTeamQuestionScorerRank(
		@PathVariable("teamId") Long teamId,
		@AuthenticationPrincipal UserEntity user) {
		QuestionRanksDto combined =
			teamQuestionRankService.getTeamQuestionScorerRank(teamId, user.getId());
		return ResponseEntity.ok(
			ApiResponse.ok(TeamQuestionRankCombinedApiResponse.from(combined)));
	}
}
