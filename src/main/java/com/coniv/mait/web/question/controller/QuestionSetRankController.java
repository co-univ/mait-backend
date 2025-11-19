package com.coniv.mait.web.question.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.dto.AnswerRankDto;
import com.coniv.mait.domain.question.service.QuestionRankService;
import com.coniv.mait.domain.solve.service.QuestionScorerService;
import com.coniv.mait.domain.solve.service.dto.QuestionScorerDto;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.CorrectorRanksApiResponse;
import com.coniv.mait.web.question.dto.ScorerRanksApiResponse;
import com.coniv.mait.web.solve.dto.QuestionScorerApiResponse;

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

	private final QuestionScorerService questionScorerService;

	@Operation(summary = "정답 개수에 따른 등수 조회 API", description = "해당 문제 셋에 정답 개수에 따른 등수 그룹을 조회한다.", parameters = {
		@Parameter(name = "type", description = "랭킹 타입", required = true, example = "CORRECT")
	})
	@GetMapping(value = "/ranks", params = "type=CORRECT")
	public ResponseEntity<ApiResponse<CorrectorRanksApiResponse>> getCorrectorsByQuestionSetId(
		@PathVariable("questionSetId") Long questionSetId) {
		List<AnswerRankDto> answerRanks = questionRankService.getCorrectorsByQuestionSetId(questionSetId);
		return ResponseEntity.ok(ApiResponse.ok(CorrectorRanksApiResponse.of(questionSetId, answerRanks)));
	}

	@Operation(summary = "문제 풀이 정답 제출 기록 조회 API")
	@GetMapping(value = "/ranks", params = "type=SCORER")
	public ResponseEntity<ApiResponse<ScorerRanksApiResponse>> getScorersByQuestionSetId(
		@PathVariable("questionSetId") Long questionSetId) {
		List<AnswerRankDto> scoreRanks = questionRankService.getScorersByQuestionSetId(questionSetId);
		return ResponseEntity.ok().body(ApiResponse.ok(ScorerRanksApiResponse.of(questionSetId, scoreRanks)));
	}

	@Operation(summary = "문제 셋 문제별 득점자 조회")
	@GetMapping("/scorers")
	public ResponseEntity<ApiResponse<List<QuestionScorerApiResponse>>> getScorers(
		@PathVariable("questionSetId") Long questionSetId) {
		List<QuestionScorerApiResponse> scorers = questionScorerService.getScorers(questionSetId).stream()
			.map(QuestionScorerApiResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok(scorers));
	}
}
