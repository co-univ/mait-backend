package com.coniv.mait.web.admin.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.onboarding.service.UserOnboardingService;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "어드민 온보딩 API")
@Profile("!prod")
@RestController
@RequestMapping("/api/v1/admin/onboarding")
@RequiredArgsConstructor
public class OnboardingDevAdminController {

	private final UserOnboardingService userOnboardingService;

	@Operation(summary = "[Dev] 온보딩 열람 이력 초기화", description = "본인 계정의 온보딩 열람 이력을 전체 삭제한다. prod 환경에서는 동작하지 않는다.")
	@DeleteMapping("/views/reset")
	public ResponseEntity<ApiResponse<Void>> resetViewHistory(@AuthenticationPrincipal MaitUser maitUser) {
		userOnboardingService.resetViewHistory(maitUser.id());
		return ResponseEntity.ok(ApiResponse.noContent());
	}
}
