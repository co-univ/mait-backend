package com.coniv.mait.web.question.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.dto.AnswerRankDto;
import com.coniv.mait.domain.question.service.QuestionRankService;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.CorrectorRanksApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "문제 셋 랭킹 관련 API")
@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}")
@RequiredArgsConstructor
public class QuestionSetRankController {

	private final QuestionRankService questionRankService;

	@Operation(summary = "정답 개수에 따른 등수 조회 API", description = "해당 문제 셋에 정답 개수에 따른 등수 그룹을 조회한다.", parameters = {
		@Parameter(name = "type", description = "랭킹 타입", required = true, example = "CORRECT")
	})
	@GetMapping(value = "/ranks", params = "type=CORRECT")
	public ResponseEntity<ApiResponse<CorrectorRanksApiResponse>> getCorrectorsByQuestionSetId(
		@PathVariable("questionSetId") Long questionSetId) {
		List<AnswerRankDto> answerRanks = questionRankService.getCorrectorsByQuestionSetId(questionSetId);
		return ResponseEntity.ok(ApiResponse.ok(CorrectorRanksApiResponse.of(questionSetId, answerRanks)));
	}
}
