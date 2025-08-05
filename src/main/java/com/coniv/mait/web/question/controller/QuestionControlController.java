package com.coniv.mait.web.question.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.QuestionControlService;
import com.coniv.mait.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}")
@RequiredArgsConstructor
@Slf4j
public class QuestionControlController {

	private final QuestionControlService questionControlService;

	/**
	 * 특정 문제의 접근을 허용
	 */
	@PostMapping("/questions/{questionId}/control/access")
	public ResponseEntity<ApiResponse<Void>> allowQuestionAccess(
		@PathVariable Long questionSetId,
		@PathVariable Long questionId) {

		questionControlService.allowQuestionAccess(questionSetId, questionId);
		return ResponseEntity.ok().build();
	}

	/**
	 * 특정 문제의 풀이를 허용
	 */
	@PostMapping("/questions/{questionId}/control/solve")
	public ResponseEntity<ApiResponse<Void>> allowQuestionSolve(
		@PathVariable Long questionSetId,
		@PathVariable Long questionId) {

		questionControlService.allowQuestionSolve(questionSetId, questionId);
		return ResponseEntity.ok().build();
	}
}
