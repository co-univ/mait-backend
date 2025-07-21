package com.coniv.mait.web.question.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.BaseUpdateQuestionApiRequest;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}/questions")
@RequiredArgsConstructor
public class QuestionController {

	private final QuestionService questionService;

	@Operation(summary = "문제 셋에 문제 저장 API")
	@PutMapping
	public ResponseEntity<ApiResponse<Void>> saveQuestionsToQuestionSet(
		@Valid @RequestBody BaseUpdateQuestionApiRequest request,
		@PathVariable("questionSetId") final Long questionSetId) {

		questionService.saveQuestionsToQuestionSet(questionSetId, request.multipleQuestions());
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
