package com.coniv.mait.web.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.admin.service.AnalyticsEventCollectService;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.admin.dto.AnalyticsEventCollectApiRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "분석 이벤트 API")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsEventController {

	private final AnalyticsEventCollectService analyticsEventCollectService;

	@Operation(summary = "분석 이벤트 수집", description =
		"제품 분석 이벤트 1건을 저장합니다. (feature_key, event_name, user_id, session_id) 조합이 이미 존재하면 중복으로 간주해 무시합니다.")
	@PostMapping("/events")
	public ResponseEntity<ApiResponse<Void>> collect(
		@Valid @RequestBody final AnalyticsEventCollectApiRequest request,
		@AuthenticationPrincipal final MaitUser maitUser) {
		analyticsEventCollectService.collect(maitUser.id(), request.featureKey(), request.eventName(),
			request.sessionId(), request.step(), request.metadata());
		return ResponseEntity.ok(ApiResponse.noContent());
	}
}
