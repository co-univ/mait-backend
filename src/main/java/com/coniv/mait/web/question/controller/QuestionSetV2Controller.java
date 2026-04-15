package com.coniv.mait.web.question.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.QuestionSetViewQueryService;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.QuestionSetViewApiResponse;
import com.coniv.mait.web.question.dto.QuestionSetViewType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 셋 API V2", description = "문제 셋 API V2")
@RestController
@RequestMapping("/api/v2/teams/{teamId}/question-sets")
@RequiredArgsConstructor
public class QuestionSetV2Controller {

	private final QuestionSetViewQueryService questionSetViewQueryService;

	@Operation(summary = "풀이 관점 문제 셋 목록 조회")
	@GetMapping("/solving")
	public ResponseEntity<ApiResponse<QuestionSetViewApiResponse>> getSolvingQuestionSets(
		@AuthenticationPrincipal MaitUser user, @PathVariable Long teamId,
		@RequestParam("type") QuestionSetViewType type) {
		QuestionSetViewApiResponse response = switch (type) {
			case LIVE_TIME -> questionSetViewQueryService.getLiveSolvingQuestionSets(teamId, user);
			case STUDY -> questionSetViewQueryService.getStudySolvingQuestionSets(teamId, user);
			case REVIEW -> questionSetViewQueryService.getReviewSolvingQuestionSets(teamId, user);
			case MAKING -> throw new UserParameterException("풀이 목록에서 지원하지 않는 type입니다: " + type);
		};
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@Operation(summary = "관리 관점 문제 셋 목록 조회")
	@GetMapping("/management")
	public ResponseEntity<ApiResponse<QuestionSetViewApiResponse>> getManagementQuestionSets(
		@AuthenticationPrincipal MaitUser user, @PathVariable Long teamId,
		@RequestParam("type") QuestionSetViewType type) {
		QuestionSetViewApiResponse response = switch (type) {
			case LIVE_TIME -> questionSetViewQueryService.getLiveManagementQuestionSets(teamId, user);
			case STUDY -> questionSetViewQueryService.getStudyManagementQuestionSets(teamId, user);
			case REVIEW -> questionSetViewQueryService.getReviewManagementQuestionSets(teamId, user);
			case MAKING -> questionSetViewQueryService.getMakingManagementQuestionSets(teamId, user);
		};
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
