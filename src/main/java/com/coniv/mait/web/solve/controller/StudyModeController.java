package com.coniv.mait.web.solve.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.solve.service.StudyModeService;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.solve.dto.QuestionAnswerSubmitApiRequest;
import com.coniv.mait.web.solve.dto.StudyAnswerDraftApiResponse;
import com.coniv.mait.web.solve.dto.UserStudyModeApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "학습모드 문제 풀이 API", description = "학습 모드 풀이 및 관리를 위해 사용되는 API")
@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}/study-mode")
@RequiredArgsConstructor
public class StudyModeController {

	private final StudyModeService studyModeService;

	@Operation(summary = "학습모드 풀이 시작 API", description = "학습 모드 - 문제 풀기 버튼 클릭 시 활용하는 API")
	@PostMapping
	public ResponseEntity<ApiResponse<UserStudyModeApiResponse>> startStudySubmission(
		@AuthenticationPrincipal MaitUser user,
		@PathVariable Long questionSetId) {
		return ResponseEntity.ok(
			ApiResponse.ok(UserStudyModeApiResponse.from(studyModeService.startStudyMode(user, questionSetId))));
	}

	@Operation(summary = "학습모드 문제별 임시 저장 목록 조회 API", description = "학습 모드 - 사용자 세션의 문제별 답안 초안을 조회합니다.")
	@GetMapping("/drafts")
	public ResponseEntity<ApiResponse<List<StudyAnswerDraftApiResponse>>> getStudyDrafts(
		@AuthenticationPrincipal MaitUser user, @PathVariable Long questionSetId) {
		return ResponseEntity.ok(ApiResponse.ok(
			StudyAnswerDraftApiResponse.from(studyModeService.getStudyAnswerDrafts(user, questionSetId))));
	}

	@Operation(summary = "학습모드 답안 업데이트 API", description = "학습 모드 - 특정 문제의 답안 초안을 업데이트합니다.")
	@PatchMapping("/drafts/{questionId}")
	public ResponseEntity<ApiResponse<StudyAnswerDraftApiResponse>> updateStudyDraft(
		@AuthenticationPrincipal MaitUser user,
		@PathVariable Long questionSetId,
		@PathVariable Long questionId,
		@Valid @RequestBody QuestionAnswerSubmitApiRequest request) throws JsonProcessingException {
		return ResponseEntity.ok(ApiResponse.ok(StudyAnswerDraftApiResponse.from(
			studyModeService.updateStudyAnswerDraft(user, questionSetId, questionId, request.getSubmitAnswers()))));
	}
}
