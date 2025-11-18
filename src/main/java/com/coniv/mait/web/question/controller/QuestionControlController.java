package com.coniv.mait.web.question.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.service.QuestionControlService;
import com.coniv.mait.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 제어 API", description = "실시간 풀이 문제 제어 관련 API")
@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}")
@RequiredArgsConstructor
public class QuestionControlController {

	private final QuestionControlService questionControlService;

	/**
	 * 특정 문제의 접근을 허용
	 */
	@Deprecated(since = "2025-11-21 정기배포 이후")
	@PostMapping("/questions/{questionId}/control/access")
	public ResponseEntity<ApiResponse<Void>> allowQuestionAccess(
		@PathVariable Long questionSetId,
		@PathVariable Long questionId) {

		questionControlService.allowQuestionAccess(questionSetId, questionId);
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	/**
	 * 특정 문제의 풀이를 허용
	 */
	@Deprecated(since = "2025-11-21 정기배포 이후")
	@PostMapping("/questions/{questionId}/control/solve")
	public ResponseEntity<ApiResponse<Void>> allowQuestionSolve(
		@PathVariable Long questionSetId,
		@PathVariable Long questionId) {

		questionControlService.allowQuestionSolve(questionSetId, questionId);
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "문제 상태 변경 API")
	@PatchMapping("/questions/{questionId}/status")
	public ResponseEntity<ApiResponse<Void>> updateQuestionStatus(
		@PathVariable("questionSetId") Long questionSetId,
		@PathVariable("questionId") Long questionId,
		@RequestParam("status") QuestionStatusType statusType) {

		questionControlService.updateQuestionStatus(questionSetId, questionId, statusType);
		return ResponseEntity.ok(ApiResponse.noContent());
	}
}
