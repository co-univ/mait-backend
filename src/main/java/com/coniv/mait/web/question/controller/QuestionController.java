package com.coniv.mait.web.question.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.CreateQuestionApiRequest;
import com.coniv.mait.web.question.dto.QuestionApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 관련 API")
@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}/questions")
@RequiredArgsConstructor
public class QuestionController {

	private final QuestionService questionService;

	@Operation(summary = "문제 셋에 문제 저장 API", description = "문제 셋에 문제를 유형별로 단건 업로드 한다.")
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> createShortQuestion(
		@Parameter(required = true, schema = @Schema(enumAsRef = true)) @RequestParam("type") QuestionType type,
		@Valid @RequestBody CreateQuestionApiRequest request,
		@PathVariable("questionSetId") final Long questionSetId) {
		questionService.createQuestion(questionSetId, type, request.toQuestionDto());
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "문제 조회 API")
	@GetMapping("/{questionId}")
	public ResponseEntity<ApiResponse<QuestionApiResponse>> getQuestion(
		@PathVariable("questionSetId") final Long questionSetId,
		@PathVariable("questionId") final Long questionId,
		@RequestParam(value = "mode", required = false) DeliveryMode mode) {
		return ResponseEntity.ok(
			ApiResponse.ok(QuestionApiResponse.from(questionService.getQuestion(questionSetId, questionId, mode))));
	}

	@Operation(summary = "문제 셋에 속한 모든 문제 조회 API")
	@GetMapping
	public ResponseEntity<ApiResponse<List<QuestionApiResponse>>> getQuestions(
		@PathVariable("questionSetId") final Long questionSetId) {
		final List<QuestionApiResponse> result = questionService.getQuestions(questionSetId).stream()
			.map(QuestionApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(result));
	}
}
