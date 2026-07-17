package com.coniv.mait.web.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.admin.service.AnalyticsEventQueryService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.admin.dto.AnalyticsEventStatsApiResponse;
import com.coniv.mait.web.admin.dto.AnalyticsFeatureApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "분석 이벤트 조회 어드민 API")
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsAdminController {

	private final AnalyticsEventQueryService analyticsEventQueryService;

	@Operation(summary = "분석 feature 목록 조회", description = "이벤트가 수집되는 분석 feature 마스터 전체를 조회한다.")
	@GetMapping("/features")
	public ResponseEntity<ApiResponse<List<AnalyticsFeatureApiResponse>>> getFeatures() {
		List<AnalyticsFeatureApiResponse> features = analyticsEventQueryService.getFeatures().stream()
			.map(AnalyticsFeatureApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(features));
	}

	@Operation(summary = "feature별 이벤트 통계 조회",
		description = "feature 1건에 대해 (event_name, step) 단위로 발생 수를 집계한다. "
			+ "event_name별로 묶어 총 발생 수와 step 분포를 함께 반환한다.")
	@GetMapping("/features/{featureId}/event-stats")
	public ResponseEntity<ApiResponse<AnalyticsEventStatsApiResponse>> getEventStats(
		@PathVariable final Long featureId) {
		return ResponseEntity.ok(ApiResponse.ok(
			AnalyticsEventStatsApiResponse.from(analyticsEventQueryService.getEventStats(featureId))));
	}
}
