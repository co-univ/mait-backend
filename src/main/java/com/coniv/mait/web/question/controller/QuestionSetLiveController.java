package com.coniv.mait.web.question.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.service.QuestionRankService;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.domain.question.service.QuestionSetLiveControlService;
import com.coniv.mait.domain.question.service.QuestionSetParticipantService;
import com.coniv.mait.domain.question.service.dto.CurrentQuestionDto;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.question.dto.CurrentQuestionApiResponse;
import com.coniv.mait.web.question.dto.ParticipantInfoApiResponse;
import com.coniv.mait.web.question.dto.ParticipantsByStatusApiResponse;
import com.coniv.mait.web.question.dto.ParticipantsCorrectAnswerRankResponse;
import com.coniv.mait.web.question.dto.QuestionSetLiveStatusResponse;
import com.coniv.mait.web.question.dto.SendWinnerRequest;
import com.coniv.mait.web.question.dto.UpdateActiveParticipantsRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/question-sets/{questionSetId}/live-status")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "실시간 풀이 문제 셋 제어 API", description = "실시간 문제 풀이를 진행하는 문제 셋 제어 관련 API")
public class QuestionSetLiveController {

	private final QuestionSetLiveControlService questionSetLiveControlService;

	private final QuestionService questionService;

	private final QuestionRankService questionRankService;

	private final QuestionSetParticipantService questionSetParticipantService;

	@Operation(summary = "실시간 문제셋 시작")
	@PatchMapping("/start")
	public ResponseEntity<ApiResponse<Void>> startLiveQuestionSet(
		@PathVariable Long questionSetId) {
		questionSetLiveControlService.startLiveQuestionSet(questionSetId);
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "실시간 문제셋 종료")
	@PatchMapping("/end")
	public ResponseEntity<ApiResponse<Void>> endLiveQuestionSet(
		@PathVariable Long questionSetId) {
		questionSetLiveControlService.endLiveQuestionSet(questionSetId);
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "실시간 문제셋 상태 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<QuestionSetLiveStatusResponse>> getLiveStatus(
		@PathVariable Long questionSetId) {
		QuestionSetOngoingStatus status = questionSetLiveControlService.getLiveStatus(questionSetId);
		return ResponseEntity.ok(ApiResponse.ok(QuestionSetLiveStatusResponse.from(questionSetId, status)));
	}

	@Operation(summary = "상태에 따른 실시간 풀이 참여 인원 조회 API", description = "현재 풀이가 가능한 인원 및 불가능한 인원 반환")
	@GetMapping("/participants")
	public ResponseEntity<ApiResponse<ParticipantsByStatusApiResponse>> getActiveParticipants(
		@PathVariable Long questionSetId) {
		return ResponseEntity.ok(ApiResponse.ok(
			ParticipantsByStatusApiResponse.from(questionSetParticipantService.getParticipants(questionSetId))));
	}

	@Operation(summary = "다음 문제 진출자 수정")
	@PutMapping("/participants")
	public ResponseEntity<ApiResponse<Void>> updateActiveParticipants(
		@PathVariable Long questionSetId,
		@RequestBody UpdateActiveParticipantsRequest request) {
		questionSetLiveControlService.updateActiveParticipants(questionSetId, request.activeUserIds());
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "우승자 전송")
	@PostMapping("/winner")
	public ResponseEntity<ApiResponse<Void>> sendWinner(
		@PathVariable Long questionSetId,
		@RequestBody SendWinnerRequest request) {
		questionSetLiveControlService.sendWinner(questionSetId, request.winnerUserIds());
		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "실시간 문제셋 정답자 랭킹")
	@GetMapping("/rank/correct")
	public ResponseEntity<ApiResponse<ParticipantsCorrectAnswerRankResponse>> getCorrectAnswerRank(
		@PathVariable Long questionSetId) {
		ParticipantsCorrectAnswerRankResponse response = ParticipantsCorrectAnswerRankResponse.from(
			questionRankService.getParticipantCorrectRank(questionSetId));
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@Operation(summary = "실시간 진행중 문제")
	@GetMapping("/current-question")
	public ResponseEntity<ApiResponse<CurrentQuestionApiResponse>> getCurrentQuestionId(
		@PathVariable Long questionSetId) {
		CurrentQuestionDto dto = questionService.findCurrentQuestion(questionSetId);
		return ResponseEntity.ok(ApiResponse.ok(CurrentQuestionApiResponse.from(dto)));
	}
}
