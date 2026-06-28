package com.coniv.mait.web.onboarding.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.onboarding.service.UserOnboardingService;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.onboarding.dto.UnviewedOnboardingScreenApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "온보딩 API")
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

	private final UserOnboardingService userOnboardingService;

	@Operation(summary = "본인이 아직 보지 않은 온보딩 화면 목록 조회",
		description = "노출 대상이며 본인이 아직 열람하지 않은 온보딩 화면 목록을 반환합니다. 역할 조건(targetTeamRole)이 반영됩니다.")
	@GetMapping("/screens/unviewed")
	public ResponseEntity<ApiResponse<List<UnviewedOnboardingScreenApiResponse>>> findUnviewedScreens(
		@AuthenticationPrincipal MaitUser maitUser) {
		List<OnboardingScreenDto> screens = userOnboardingService.getUnviewedScreens(maitUser.id());
		return ResponseEntity.ok(
			ApiResponse.ok(screens.stream().map(UnviewedOnboardingScreenApiResponse::from).toList()));
	}
}
