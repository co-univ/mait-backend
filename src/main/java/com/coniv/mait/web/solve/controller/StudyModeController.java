package com.coniv.mait.web.solve.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.solve.service.StudyModeService;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.solve.dto.UserStudyModeApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}
