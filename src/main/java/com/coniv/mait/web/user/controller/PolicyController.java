package com.coniv.mait.web.user.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.service.PolicyService;
import com.coniv.mait.domain.user.service.dto.PolicyDto;
import com.coniv.mait.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

	private final PolicyService policyService;

	@Operation(summary = "타이밍별 최신 정책 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<PolicyDto>>> getLatestPolicies(
		@RequestParam PolicyTiming timing) {
		List<PolicyDto> policies = policyService.findLatestPolicies(timing);
		return ResponseEntity.ok(ApiResponse.ok(policies));
	}
}
