package com.coniv.mait.web.question.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.coniv.mait.domain.question.enums.AiRequestStatus;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.service.QuestionSetMaterialService;
import com.coniv.mait.domain.question.service.QuestionSetService;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.domain.question.service.dto.QuestionSetMaterialDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.AiRequestStatusApiResponse;
import com.coniv.mait.web.question.dto.CreateQuestionSetApiRequest;
import com.coniv.mait.web.question.dto.CreateQuestionSetApiResponse;
import com.coniv.mait.web.question.dto.QuestionSetApiResponse;
import com.coniv.mait.web.question.dto.QuestionSetContainer;
import com.coniv.mait.web.question.dto.QuestionSetMaterialApiResponse;
import com.coniv.mait.web.question.dto.QuestionSetsApiResponse;
import com.coniv.mait.web.question.dto.QuestionValidationApiResponse;
import com.coniv.mait.web.question.dto.UpdateQuestionSetApiRequest;
import com.coniv.mait.web.question.dto.UpdateQuestionSetFieldApiRequest;
import com.coniv.mait.web.question.dto.UpdateQuestionSetReviewApiRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 셋 API", description = "문제 셋 API")
@RestController
@RequestMapping("/api/v1/question-sets")
@RequiredArgsConstructor
public class QuestionSetController {

	private final QuestionSetService questionSetService;

	private final QuestionSetMaterialService questionSetMaterialService;

	@Operation(summary = "문제 셋 생성 API", description = "새로운 문제 셋을 생성합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<CreateQuestionSetApiResponse>> createQuestionSet(
		@AuthenticationPrincipal UserEntity user,
		@Valid @RequestBody CreateQuestionSetApiRequest request) {
		QuestionSetDto questionSetDto = questionSetService.createQuestionSet(request.toQuestionSetDto(),
			request.counts(), request.materials(), request.instruction(), request.difficulty(), user.getId());
		return ResponseEntity.status(HttpStatus.OK)
			.body(ApiResponse.ok(CreateQuestionSetApiResponse.from(questionSetDto)));
	}

	@Operation(summary = "문제 셋에 사용될 파일 업로드 API", description = "문제 셋 생성 과정에서 사용될 파일을 업로드합니다.")
	@PostMapping(value = "/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<QuestionSetMaterialApiResponse>> uploadQuestionSetFiles(
		@RequestPart("material") MultipartFile material) {
		QuestionSetMaterialDto materialDto = questionSetMaterialService.uploadQuestionSetMaterial(material);
		return ResponseEntity.status(HttpStatus.OK)
			.body(ApiResponse.ok(QuestionSetMaterialApiResponse.from(materialDto)));
	}

	@Operation(summary = "문제 셋 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<QuestionSetsApiResponse>> getQuestionSets(
		@RequestParam(value = "mode") DeliveryMode mode,
		@RequestParam("teamId") Long teamId) {
		QuestionSetContainer questionSets = questionSetService.getQuestionSets(teamId, mode);
		QuestionSetsApiResponse response = QuestionSetsApiResponse.of(mode, questionSets);
		return ResponseEntity.ok(ApiResponse.ok(response));
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
	public ResponseEntity<ApiResponse<QuestionSetApiResponse>> completeQuestionSet(
		@PathVariable("questionSetId") Long questionSetId,
		@Valid @RequestBody UpdateQuestionSetApiRequest request) {
		return ResponseEntity.ok(ApiResponse.ok(QuestionSetApiResponse.from(
			questionSetService.completeQuestionSet(questionSetId, request.title(), request.subject(),
				request.mode(), request.difficulty(), request.visibility()))));
	}

	@Operation(summary = "문제 셋 제목 단건 수정 API", description = "연필 버튼 클릭을 통한 문제 셋 단건 수정")
	@PatchMapping("/{questionSetId}")
	public ResponseEntity<ApiResponse<Void>> updateQuestionSet(
		@RequestBody @Valid UpdateQuestionSetFieldApiRequest request,
		@PathVariable("questionSetId") Long questionSetId) {
		questionSetService.updateQuestionSetField(questionSetId, request.title());
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "문제 셋 전체 문제 검증 API", description = "문제 셋 전체 문제를 검증 후 조건을 만족하지 않는 문제 목록을 반환한다.")
	@GetMapping("/validate")
	public ResponseEntity<ApiResponse<List<QuestionValidationApiResponse>>> validateQuestionSet(
		@RequestParam("questionSetId") Long questionSetId) {
		List<QuestionValidationApiResponse> responses = questionSetService.validateQuestionSet(questionSetId)
			.stream()
			.map(QuestionValidationApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(responses));
	}

	@Operation(summary = "AI 문제 제작 상태 조회", description = "AI 문제 제작 상태를 조회합니다.")
	@GetMapping("/{questionSetId}/ai-status")
	public ResponseEntity<ApiResponse<AiRequestStatusApiResponse>> getAiRequestStatus(
		@PathVariable("questionSetId") Long questionSetId) {
		AiRequestStatus status = questionSetService.getAiRequestStatus(questionSetId);
		return ResponseEntity.ok(ApiResponse.ok(AiRequestStatusApiResponse.of(questionSetId, status)));
	}

	@Operation(summary = "종료된 문제를 복습 상태로 전환", description = "종료된 학습/실시간 모드의 문제를 복습 상태로 전환한다.")
	@PatchMapping("/{questionSetId}/review")
	public ResponseEntity<ApiResponse<Void>> updateToReviewMode(@PathVariable("questionSetId") Long questionSetId,
		@RequestBody @Valid UpdateQuestionSetReviewApiRequest request) {
		questionSetService.updateQuestionSetToReviewMode(questionSetId, request.visibility());
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "종료된 문제 셋을 실시간 풀이 진행중으로 변경", description = "종료된 문제 셋을 다시 실시간 상태로 되돌린다.")
	@PatchMapping("/{questionSetId}/restart")
	public ResponseEntity<ApiResponse<Void>> restartQuestionSet(@PathVariable("questionSetId") Long questionSetId) {
		questionSetService.restartQuestionSet(questionSetId);
		return ResponseEntity.ok(ApiResponse.noContent());
	}
}
