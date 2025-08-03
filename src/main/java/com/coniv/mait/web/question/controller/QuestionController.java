package com.coniv.mait.web.question.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.CreateQuestionApiRequest;
import com.coniv.mait.web.question.dto.QuestionApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 관련 API")
@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}/questions")
@RequiredArgsConstructor
public class QuestionController {

	private final QuestionService questionService;

	@Operation(summary = "문제 셋에 주관식 문제 저장 API")
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> createShortQuestion(
		@RequestParam("type") QuestionType type,
		@Valid @RequestBody CreateQuestionApiRequest request,
		@PathVariable("questionSetId") final Long questionSetId) {
		questionService.createQuestion(questionSetId, type, request.toQuestionDto());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}

	@Operation(summary = "문제 조회 API")
	@GetMapping("/{questionId}")
	public ResponseEntity<ApiResponse<?>> getQuestion(
		@PathVariable("questionSetId") final Long questionSetId,
		@PathVariable("questionId") final Long questionId) {
		return ResponseEntity.ok(
			ApiResponse.ok(QuestionApiResponse.from(questionService.getQuestion(questionSetId, questionId))));
	}
}
