package com.coniv.mait.web.question.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.QuestionSetService;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.CreateQuestionSetApiRequest;
import com.coniv.mait.web.question.dto.CreateQuestionSetApiResponse;
import com.coniv.mait.web.question.dto.QuestionSetApiResponse;
import com.coniv.mait.web.question.dto.UpdateQuestionSetApiRequest;

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

	@Operation(summary = "문제 셋 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<QuestionSetApiResponse>>> getQuestionSets(
		@RequestParam("teamId") Long teamId) {
		List<QuestionSetApiResponse> responses = questionSetService.getQuestionSets(teamId)
			.stream()
			.map(QuestionSetApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(responses));
	}

	@Operation(summary = "문제 셋 단건 조회")
	@GetMapping("/{questionSetId}")
	public ResponseEntity<ApiResponse<QuestionSetApiResponse>> getQuestionSet(
		@PathVariable("questionSetId") Long questionSetId) {
		QuestionSetDto questionSetDto = questionSetService.getQuestionSet(questionSetId);
		return ResponseEntity.ok(ApiResponse.ok(QuestionSetApiResponse.from(questionSetDto)));
	}

	@Operation(summary = "문제 셋을 최종 저장 API", description = "문제 셋을 제작 완료 상태로 변경")
	@PutMapping("/{questionSetId}")
	public ResponseEntity<ApiResponse<QuestionSetApiResponse>> updateQuestionSets(
		@PathVariable("questionSetId") Long questionSetId,
		@Valid @RequestBody UpdateQuestionSetApiRequest request) {
		return ResponseEntity.ok(ApiResponse.ok(QuestionSetApiResponse.from(
			questionSetService.completeQuestionSet(questionSetId, request.title(), request.subject(), request.mode(),
				request.levelDescription(), request.visibility()))));
	}
}
