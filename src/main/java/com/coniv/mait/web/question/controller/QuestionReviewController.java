package com.coniv.mait.web.question.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.QuestionReviewService;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.LastViewedQuestionApiRequest;
import com.coniv.mait.web.question.dto.QuestionApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}")
@RequiredArgsConstructor
public class QuestionReviewController {

	private final QuestionReviewService questionReviewService;

	@Operation(summary = "마지막으로 풀이한 문제 조회 API", description = "복습 모드에서 해당 문제 셋에 마지막으로 조회한 문제를 반환하고 없으면 1번 문제를 반환")
	@GetMapping("/questions/last-viewed")
	public ResponseEntity<ApiResponse<QuestionApiResponse>> getLastViewedQuestion(
		@PathVariable("questionSetId") Long questionSetId,
		@AuthenticationPrincipal UserEntity user) {
		return ResponseEntity.ok(ApiResponse.ok(
			QuestionApiResponse.from(questionReviewService.getLastViewedQuestionInReview(questionSetId, user.getId()))
		));
	}

	@Operation(summary = "마지막으로 풀이한 문제 업데이트 API", description = "복습 모드에서 사용자가 마지막으로 조회한 문제를 업데이트")
	@PutMapping("/questions/last-viewed")
	public ResponseEntity<ApiResponse<Void>> updateLastViewedQuestion(
		@PathVariable("questionSetId") Long questionSetId,
		@RequestBody LastViewedQuestionApiRequest request,
		@AuthenticationPrincipal UserEntity user) {
		questionReviewService.updateLastViewedQuestion(questionSetId, request.questionId(), user.getId());
		return ResponseEntity.ok(ApiResponse.noContent());
	}
}
