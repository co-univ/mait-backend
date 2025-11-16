package com.coniv.mait.web.solve.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.solve.service.QuestionAnswerSubmitService;
import com.coniv.mait.domain.solve.service.QuestionScorerService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.solve.dto.QuestionAnswerSubmitApiRequest;
import com.coniv.mait.web.solve.dto.QuestionAnswerSubmitApiResponse;
import com.coniv.mait.web.solve.dto.QuestionAnswerSubmitRecordApiResponse;
import com.coniv.mait.web.solve.dto.QuestionScorerApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 풀이 API")
@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}/questions/{questionId}")
@RequiredArgsConstructor
public class QuestionAnswerSubmitController {

	private final QuestionAnswerSubmitService questionAnswerSubmitService;

	private final QuestionScorerService questionScorerService;

	@Operation(summary = "문제 풀이 정답 제출 API")
	@PostMapping("/submit")
	public ResponseEntity<ApiResponse<QuestionAnswerSubmitApiResponse>> submitAnswer(
		@Valid @RequestBody QuestionAnswerSubmitApiRequest request,
		@PathVariable("questionSetId") Long questionSetId, @PathVariable("questionId") Long questionId) throws
		JsonProcessingException {
		return ResponseEntity.ok().body(ApiResponse.ok(
			QuestionAnswerSubmitApiResponse.from(questionAnswerSubmitService.submitAnswer(questionSetId, questionId,
				request.getUserId(), request.getSubmitAnswers()))));
	}

	@Operation(summary = "문제별 득점자 조회 API")
	@GetMapping("/scorer")
	public ResponseEntity<ApiResponse<QuestionScorerApiResponse>> getScorer(
		@PathVariable("questionSetId") Long questionSetId,
		@PathVariable("questionId") Long questionId
	) {
		return ResponseEntity.ok().body(ApiResponse.ok(
			QuestionScorerApiResponse.from(questionScorerService.getScorer(questionSetId, questionId))));
	}

	@Operation(summary = "문제 풀이 정답 제출 기록 조회 API")
	@GetMapping("/submit-records")
	public ResponseEntity<ApiResponse<List<QuestionAnswerSubmitRecordApiResponse>>> getSubmitRecords(
		@PathVariable("questionSetId") Long questionSetId, @PathVariable("questionId") Long questionId) {
		return ResponseEntity.ok().body(ApiResponse.ok(
			QuestionAnswerSubmitRecordApiResponse.from(
				questionAnswerSubmitService.getSubmitRecords(questionSetId, questionId))));
	}
}
