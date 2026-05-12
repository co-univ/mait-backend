package com.coniv.mait.web.question.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.coniv.mait.web.question.dto.RestoreQuestionSetCategoryApiRequest;
import com.coniv.mait.web.question.dto.UpdateQuestionSetCategoryApiRequest;

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

	@Operation(summary = "문제 셋 카테고리 검색 API", description = "팀의 활성 카테고리를 이름 부분 일치로 검색합니다. 팀 멤버만 조회 가능합니다.")
	@GetMapping("/search")
	public ResponseEntity<ApiResponse<List<QuestionSetCategoryApiResponse>>> searchCategories(
		@AuthenticationPrincipal MaitUser user, @RequestParam("teamId") Long teamId,
		@RequestParam("keyword") String keyword) {
		List<QuestionSetCategoryApiResponse> categories = questionSetCategoryService.searchCategories(teamId, user.id(),
				keyword)
			.stream()
			.map(QuestionSetCategoryApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(categories));
	}

	@Operation(summary = "문제 셋 카테고리 이름 수정 API",
		description = "활성 팀 카테고리의 이름을 수정합니다. 기존 문제 셋 매핑은 유지됩니다.")
	@PatchMapping("/{categoryId}")
	public ResponseEntity<ApiResponse<QuestionSetCategoryApiResponse>> updateCategoryName(
		@AuthenticationPrincipal MaitUser user, @PathVariable Long categoryId,
		@Valid @RequestBody UpdateQuestionSetCategoryApiRequest request) {
		QuestionSetCategoryDto category = questionSetCategoryService.updateCategoryName(categoryId, request.name(),
			user.id());
		return ResponseEntity.ok(ApiResponse.ok(QuestionSetCategoryApiResponse.from(category)));
	}

	@Operation(summary = "문제 셋 카테고리 삭제 API", description = "팀 카테고리를 soft delete 합니다. 이미 삭제된 카테고리는 멱등 처리됩니다.")
	@DeleteMapping("/{categoryId}")
	public ResponseEntity<ApiResponse<Void>> deleteCategory(
		@AuthenticationPrincipal MaitUser user, @PathVariable Long categoryId) {
		questionSetCategoryService.deleteCategory(categoryId, user.id());
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "문제 셋 카테고리 복구 API",
		description = "팀과 이름으로 식별되는 soft delete 된 카테고리를 복구합니다. 동일 이름 활성 카테고리가 존재하면 409 를 반환합니다.")
	@PostMapping("/restore")
	public ResponseEntity<ApiResponse<QuestionSetCategoryApiResponse>> restoreCategory(
		@AuthenticationPrincipal MaitUser user, @Valid @RequestBody RestoreQuestionSetCategoryApiRequest request) {
		QuestionSetCategoryDto category = questionSetCategoryService.restoreCategory(
			request.teamId(), request.name(), user.id());
		return ResponseEntity.ok(ApiResponse.ok(QuestionSetCategoryApiResponse.from(category)));
	}
}
