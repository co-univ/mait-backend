package com.coniv.mait.web.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.statistic.service.QuestionAnswerStatisticService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.admin.dto.QuestionAnswerDistributionApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 통계 어드민 API")
@RestController
@RequestMapping("/api/v1/admin/question-sets/{questionSetId}/questions/{questionId}/statistics")
@RequiredArgsConstructor
public class QuestionStatisticAdminController {

	private final QuestionAnswerStatisticService questionAnswerStatisticService;

	@Operation(summary = "문제별 제출 답안 분포 통계 조회 API",
		description = "문제 유형과 무관하게, 동일한 제출 답안끼리 그룹화하여 그룹별 제출 수/정오답/제출 답안을 조회한다. "
			+ "firstSubmitOnly=true 면 유저당 최초 제출만 기준으로 응답을 구성한다.")
	@GetMapping("/answer-distribution")
	public ResponseEntity<ApiResponse<QuestionAnswerDistributionApiResponse>> getAnswerDistribution(
		@PathVariable Long questionSetId, @PathVariable Long questionId,
		@RequestParam(defaultValue = "false") boolean firstSubmitOnly) {
		return ResponseEntity.ok().body(ApiResponse.ok(
			QuestionAnswerDistributionApiResponse.from(
				questionAnswerStatisticService.getAnswerDistribution(questionSetId, questionId, firstSubmitOnly))));
	}
}
