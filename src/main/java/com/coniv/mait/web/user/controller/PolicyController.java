package com.coniv.mait.web.user.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.service.PolicyService;
import com.coniv.mait.domain.user.service.dto.PolicyDto;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.user.dto.CheckPoliciesApiRequest;
import com.coniv.mait.web.user.dto.CheckPoliciesApiResponse;
import com.coniv.mait.web.user.dto.LatestPoliciesApiResponse;
import com.coniv.mait.web.user.dto.UnconfirmedPoliciesApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

	private final PolicyService policyService;

	@Operation(summary = "타이밍별 최신 정책 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<LatestPoliciesApiResponse>>> findLatestPolicies(
		@RequestParam("timing") PolicyTiming timing) {
		List<PolicyDto> policies = policyService.findLatestPolicies(timing);

		return ResponseEntity.ok(ApiResponse.ok(policies.stream().map(LatestPoliciesApiResponse::from).toList()));
	}

	@Operation(summary = "타이밍별 미확인 정책 목록 조회")
	@GetMapping("/unchecked/{userId}")
	public ResponseEntity<ApiResponse<List<UnconfirmedPoliciesApiResponse>>> findUnConfirmedPolicies(
		@RequestParam("timing") PolicyTiming timing, @PathVariable("userId") Long userId) {
		List<PolicyDto> policies = policyService.findUnConfirmedPolicies(userId, timing);

		return ResponseEntity.ok(ApiResponse.ok(policies.stream().map(UnconfirmedPoliciesApiResponse::from).toList()));
	}

	@Operation(summary = "정책 동의/미동의 처리", description = "여러 정책에 대해 동의 또는 미동의를 한번에 처리합니다.")
	@PostMapping("/check")
	public ResponseEntity<ApiResponse<CheckPoliciesApiResponse>> checkPolicies(
		@Valid @RequestBody CheckPoliciesApiRequest request,
		@AuthenticationPrincipal UserEntity userPrincipal) {

		LocalDateTime checkTime = policyService.checkPolicies(userPrincipal.getId(), request.policyChecks());

		return ResponseEntity.ok(ApiResponse.ok(CheckPoliciesApiResponse.of(checkTime)));
	}
}
