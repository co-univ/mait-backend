package com.coniv.mait.web.question.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.QuestionSetService;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.CreateQuestionSetApiRequest;
import com.coniv.mait.web.question.dto.CreateQuestionSetApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "QuestionSet", description = "문제 셋 API")
@RestController
@RequestMapping("/api/v1/question-sets")
@RequiredArgsConstructor
public class QuestionSetController {

	private final QuestionSetService questionSetService;

	@Operation(summary = "문제 셋 생성 API", description = "새로운 문제 셋을 생성합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<CreateQuestionSetApiResponse>> createQuestionSet(
		@Valid @RequestBody CreateQuestionSetApiRequest request) {
		QuestionSetDto questionSetDto = questionSetService.createQuestionSet(request.subject(), request.creationType());
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.ok(CreateQuestionSetApiResponse.from(questionSetDto)));
	}
}
