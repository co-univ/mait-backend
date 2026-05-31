package com.coniv.mait.web.statistic.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.statistic.service.SolvingResultService;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.statistic.dto.QuestionSetStatisticApiResponse;
import com.coniv.mait.web.statistic.dto.UserSolvingResultApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "내 풀이 기록 조회 API")
@RestController
@RequestMapping("/api/v1/question-sets")
@RequiredArgsConstructor
public class QuestionSolvingRecordController {

	private final SolvingResultService solvingResultService;

	@Operation(summary = "본인의 문제 셋 풀이 기록 조회 API",
		description = "특정 문제 셋에 대한 본인의 풀이 기록(전체 문제 수, 맞춘 문제 수, 100점 만점 점수, 문제별 상세)을 조회한다.")
	@GetMapping("/{questionSetId}/user/result")
	public ResponseEntity<ApiResponse<UserSolvingResultApiResponse>> getMySolveRecord(
		@AuthenticationPrincipal MaitUser user, @PathVariable Long questionSetId) {
		return ResponseEntity.ok(ApiResponse.ok(
			UserSolvingResultApiResponse.from(solvingResultService.getSolvingResults(user, questionSetId))));
	}

	@Operation(summary = "본인이 특정 팀에서 풀었던 문제 셋 정보")
	@GetMapping("/statistics")
	public ResponseEntity<ApiResponse<List<QuestionSetStatisticApiResponse>>> getSolvedQuestionSet(
		@AuthenticationPrincipal MaitUser maitUser, @RequestParam("teamId") Long teamId) {
		List<QuestionSetStatisticApiResponse> response = solvingResultService.getTeamQuestionSetStatistics(
				maitUser, teamId).stream()
			.map(QuestionSetStatisticApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
