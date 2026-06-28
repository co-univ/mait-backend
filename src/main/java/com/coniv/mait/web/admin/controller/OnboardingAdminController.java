package com.coniv.mait.web.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.onboarding.service.OnboardingScreenService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.admin.dto.OnboardingScreenApiResponse;
import com.coniv.mait.web.admin.dto.OnboardingScreenUploadApiRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "어드민 온보딩 API")
@RestController
@RequestMapping("/api/v1/admin/onboarding")
@RequiredArgsConstructor
public class OnboardingAdminController {

	private final OnboardingScreenService onboardingScreenService;

	@Operation(summary = "온보딩 화면 카탈로그 등록 API", description = "온보딩 화면 데이터를 등록한다. 동일 code 가 존재하면 갱신된다.")
	@PostMapping("/screens")
	public ResponseEntity<ApiResponse<OnboardingScreenApiResponse>> uploadScreen(
		@Valid @RequestBody OnboardingScreenUploadApiRequest request) {
		OnboardingScreenApiResponse response = OnboardingScreenApiResponse.from(
			onboardingScreenService.uploadScreen(request.code(), request.title(), request.targetTeamRole()));
		return ResponseEntity.ok().body(ApiResponse.ok(response));
	}
}
