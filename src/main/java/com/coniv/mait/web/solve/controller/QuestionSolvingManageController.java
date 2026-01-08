package com.coniv.mait.web.solve.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.solve.service.QuestionSolvingManageService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.solve.dto.UpdateQuestionAnswerApiRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 풀이 관리 API", description = "어드민에서 사용하는 풀이 관리 API")
@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}/questions")
@RequiredArgsConstructor
public class QuestionSolvingManageController {

	private final QuestionSolvingManageService questionSolvingManageService;

	@Operation(summary = "문제 정답 추가 API", description = "실시간 풀이 진행 중 특정 문제의 정답을 추가한다.")
	@PostMapping("/{questionId}/answers")
	public ResponseEntity<ApiResponse<Void>> regradeQuestionSubmitRecords(
		@PathVariable("questionSetId") Long questionSetId,
		@PathVariable("questionId") Long questionId,
		@RequestBody @Valid UpdateQuestionAnswerApiRequest request) {

		questionSolvingManageService.updateQuestionAnswers(questionSetId, questionId, request.payload());
		return ResponseEntity.ok(ApiResponse.noContent());
	}
}
