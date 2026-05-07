package com.coniv.mait.web.question.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.service.QuestionSetCategoryService;
import com.coniv.mait.domain.question.service.dto.QuestionSetCategoryDto;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.CreateQuestionSetCategoryApiRequest;
import com.coniv.mait.web.question.dto.QuestionSetCategoryApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 셋 카테고리 API", description = "팀 단위 문제 셋 카테고리 관리 API")
@RestController
@RequestMapping("/api/v1/question-sets/categories")
@RequiredArgsConstructor
public class QuestionSetCategoryController {

	private final QuestionSetCategoryService questionSetCategoryService;

	@Operation(summary = "문제 셋 카테고리 생성 API", description = "팀 단위로 새로운 문제 셋 카테고리를 생성합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<QuestionSetCategoryApiResponse>> createCategory(
		@AuthenticationPrincipal MaitUser user, @Valid @RequestBody CreateQuestionSetCategoryApiRequest request) {
		QuestionSetCategoryDto category = questionSetCategoryService.createCategory(request.teamId(), request.name(),
			user.id());
		return ResponseEntity.ok(ApiResponse.ok(QuestionSetCategoryApiResponse.from(category)));
	}

	@Operation(summary = "문제 셋 카테고리 목록 조회 API", description = "팀의 활성 카테고리 목록을 조회합니다. 팀 멤버만 조회 가능합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<QuestionSetCategoryApiResponse>>> getCategories(
		@AuthenticationPrincipal MaitUser user, @RequestParam("teamId") Long teamId) {
		List<QuestionSetCategoryApiResponse> categories = questionSetCategoryService.getCategories(teamId, user.id())
			.stream()
			.map(QuestionSetCategoryApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(categories));
	}
}
