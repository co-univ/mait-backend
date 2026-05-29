package com.coniv.mait.web.solve.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.solve.service.SolvingResultService;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.solve.dto.UserSolvingResultApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "내 풀이 기록 조회 API")
@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}")
@RequiredArgsConstructor
public class QuestionSolvingRecordController {

	private final SolvingResultService solvingResultService;

	@Operation(summary = "본인의 문제 셋 풀이 기록 조회 API",
		description = "특정 문제 셋에 대한 본인의 풀이 기록(전체 문제 수, 맞춘 문제 수, 100점 만점 점수, 문제별 상세)을 조회한다.")
	@GetMapping("/user/result")
	public ResponseEntity<ApiResponse<UserSolvingResultApiResponse>> getMySolveRecord(
		@AuthenticationPrincipal MaitUser user, @PathVariable Long questionSetId) {
		return ResponseEntity.ok(ApiResponse.ok(
			UserSolvingResultApiResponse.from(solvingResultService.getSolvingResults(user, questionSetId))));
	}
}
