package com.coniv.mait.web.statistic.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.statistic.service.QuestionStatisticService;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.statistic.dto.QuestionStatisticApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 셋 통계 조회 API")
@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}")
@RequiredArgsConstructor
public class QuestionStatisticController {

	private final QuestionStatisticService questionStatisticService;

	@Operation(summary = "문제 셋 문제별 오답률 조회 API", description = "문제 셋의 문제별 오답률(최초 제출 기준)을 높은 순으로 조회한다.")
	@GetMapping("/questions/wrong-rates")
	public ResponseEntity<ApiResponse<List<QuestionStatisticApiResponse>>> getWrongRates(
		@AuthenticationPrincipal MaitUser maitUser, @PathVariable Long questionSetId) {
		List<QuestionStatisticApiResponse> response = questionStatisticService.getWrongRates(questionSetId, maitUser)
			.stream()
			.map(QuestionStatisticApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
