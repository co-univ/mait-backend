package com.coniv.mait.web.question.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.enums.QuestionSetLiveStatus;
import com.coniv.mait.domain.question.service.QuestionSetLiveControlService;
import com.coniv.mait.web.question.dto.QuestionSetLiveStatusResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/question-sets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "QuestionSet Live Control", description = "실시간 문제셋 제어 API")
public class QuestionSetLiveController {

	private final QuestionSetLiveControlService questionSetLiveControlService;

	@Operation(summary = "실시간 문제셋 시작")
	@PatchMapping("/{questionSetId}/live/start")
	public ResponseEntity<Void> startLiveQuestionSet(
		@PathVariable Long questionSetId) {
		questionSetLiveControlService.startLiveQuestionSet(questionSetId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "실시간 문제셋 종료")
	@PatchMapping("/{questionSetId}/live/end")
	public ResponseEntity<Void> endLiveQuestionSet(
		@PathVariable Long questionSetId) {
		questionSetLiveControlService.endLiveQuestionSet(questionSetId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "실시간 문제셋 상태 조회")
	@GetMapping("/{questionSetId}/live/status")
	public ResponseEntity<QuestionSetLiveStatusResponse> getLiveStatus(
		@PathVariable Long questionSetId) {
		QuestionSetLiveStatus status = questionSetLiveControlService.getLiveStatus(questionSetId);
		return ResponseEntity.ok(QuestionSetLiveStatusResponse.from(questionSetId, status));
	}
}
