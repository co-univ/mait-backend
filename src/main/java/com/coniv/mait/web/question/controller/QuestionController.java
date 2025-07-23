package com.coniv.mait.web.question.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.CreateMultipleQuestionApiRequest;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}/questions")
@RequiredArgsConstructor
public class QuestionController {

	private final QuestionService questionService;

	@Operation(summary = "문제 셋에 객관식 문제 저장 API")
	@PostMapping(params = "type=multiple")
	public ResponseEntity<ApiResponse<Void>> createMultipleQuestion(
		@Valid @RequestBody CreateMultipleQuestionApiRequest request,
		@PathVariable("questionSetId") final Long questionSetId) {
		questionService.createMultipleQuestion(questionSetId, request.multipleQuestionDto());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
