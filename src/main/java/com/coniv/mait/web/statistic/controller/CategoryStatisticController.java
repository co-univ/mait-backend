package com.coniv.mait.web.statistic.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.statistic.service.CategoryStatisticService;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.statistic.dto.CategoryCorrectRateApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "카테고리 통계 조회 API")
@RestController
@RequestMapping("/api/v1/teams/{teamId}/categories")
@RequiredArgsConstructor
public class CategoryStatisticController {

	private final CategoryStatisticService categoryStatisticService;

	@Operation(summary = "카테고리별 정답률 조회 API",
		description = "카테고리에 속한 종료된 문제 셋들의 모든 문제를 통째로 모아 본인 정답률과 전체 평균 정답률(첫 제출 기준)을 조회한다.")
	@GetMapping("/{categoryId}/correct-rate")
	public ResponseEntity<ApiResponse<CategoryCorrectRateApiResponse>> getCategoryCorrectRate(
		@AuthenticationPrincipal MaitUser user, @PathVariable Long teamId, @PathVariable Long categoryId) {
		CategoryCorrectRateApiResponse response = CategoryCorrectRateApiResponse.from(
			categoryStatisticService.getCategoryCorrectRate(user, teamId, categoryId));
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@Operation(summary = "팀 카테고리별 정답률 랭킹 조회 API",
		description = "팀에 속한 모든 카테고리의 본인 정답률을 높은 순으로 조회한다. 미응시 카테고리는 정답률 null로 맨 뒤에 위치한다.")
	@GetMapping("/correct-rates")
	public ResponseEntity<ApiResponse<List<CategoryCorrectRateApiResponse>>> getCategoryCorrectRates(
		@AuthenticationPrincipal MaitUser user, @PathVariable Long teamId) {
		List<CategoryCorrectRateApiResponse> response = categoryStatisticService.getCategoryCorrectRates(user, teamId)
			.stream()
			.map(CategoryCorrectRateApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
