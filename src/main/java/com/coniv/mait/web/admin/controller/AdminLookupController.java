package com.coniv.mait.web.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.admin.service.AdminLookupService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.admin.dto.AdminQuestionSetApiResponse;
import com.coniv.mait.web.admin.dto.AdminTeamApiResponse;
import com.coniv.mait.web.question.dto.QuestionApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "어드민 조회 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminLookupController {

	private final AdminLookupService adminLookupService;

	@Operation(summary = "모든 팀 목록 조회 API", description = "삭제되지 않은 모든 팀(개인 워크스페이스 포함)을 조회한다.")
	@GetMapping("/teams")
	public ResponseEntity<ApiResponse<List<AdminTeamApiResponse>>> getAllTeams() {
		List<AdminTeamApiResponse> response = adminLookupService.getAllTeams().stream()
			.map(AdminTeamApiResponse::from)
			.toList();
		return ResponseEntity.ok().body(ApiResponse.ok(response));
	}

	@Operation(summary = "특정 팀의 문제 셋 목록 조회 API")
	@GetMapping("/teams/{teamId}/question-sets")
	public ResponseEntity<ApiResponse<List<AdminQuestionSetApiResponse>>> getQuestionSets(
		@PathVariable Long teamId) {
		List<AdminQuestionSetApiResponse> response = adminLookupService.getQuestionSets(teamId).stream()
			.map(AdminQuestionSetApiResponse::from)
			.toList();
		return ResponseEntity.ok().body(ApiResponse.ok(response));
	}

	@Operation(summary = "특정 문제 셋의 문제 목록 조회 API", description = "어드민 조회이므로 실제 정답이 포함되어 응답된다.")
	@GetMapping("/question-sets/{questionSetId}/questions")
	public ResponseEntity<ApiResponse<List<QuestionApiResponse>>> getQuestions(
		@PathVariable Long questionSetId) {
		List<QuestionApiResponse> response = adminLookupService.getQuestions(questionSetId).stream()
			.map(QuestionApiResponse::from)
			.toList();
		return ResponseEntity.ok().body(ApiResponse.ok(response));
	}
}
