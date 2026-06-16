package com.coniv.mait.web.statistic.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.solve.service.dto.QuestionSolveResultDto;
import com.coniv.mait.domain.statistic.service.SolvingResultService;
import com.coniv.mait.domain.statistic.service.dto.MySolveRecordDto;
import com.coniv.mait.domain.statistic.service.dto.QuestionSetStatisticDto;
import com.coniv.mait.domain.statistic.service.dto.QuestionSetWinnerDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.statistic.controller.QuestionSolvingRecordController;

@WebMvcTest(controllers = QuestionSolvingRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionSolvingRecordControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SolvingResultService solvingResultService;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@Test
	@DisplayName("본인 풀이 기록 조회 성공")
	void getMySolveRecord_Success() throws Exception {
		// given
		Long questionSetId = 1L;

		MySolveRecordDto recordDto = MySolveRecordDto.builder()
			.questionSetId(questionSetId)
			.solveMode(QuestionSetSolveMode.STUDY)
			.totalCount(2)
			.correctCount(1)
			.score(50.0)
			.results(List.of(
				QuestionSolveResultDto.builder()
					.questionId(11L)
					.isCorrect(true)
					.submittedAnswer("{\"type\":\"SHORT\",\"submitAnswers\":[\"답안\"]}")
					.build(),
				QuestionSolveResultDto.builder()
					.questionId(12L)
					.isCorrect(false)
					.submittedAnswer(null)
					.build()))
			.build();

		given(solvingResultService.getSolvingResults(any(), eq(questionSetId))).willReturn(recordDto);

		// when & then
		mockMvc.perform(
				get("/api/v1/question-sets/{questionSetId}/user/result", questionSetId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.questionSetId").value(questionSetId),
				jsonPath("$.data.solveMode").value("STUDY"),
				jsonPath("$.data.totalCount").value(2),
				jsonPath("$.data.correctCount").value(1),
				jsonPath("$.data.score").value(50.0),
				jsonPath("$.data.results[0].questionId").value(11),
				jsonPath("$.data.results[0].isCorrect").value(true),
				jsonPath("$.data.results[1].questionId").value(12),
				jsonPath("$.data.results[1].isCorrect").value(false));

		verify(solvingResultService).getSolvingResults(any(), eq(questionSetId));
	}

	@Test
	@DisplayName("팀 문제 셋 통계 조회 성공 - 우승자/내 정답률/평균 정답률 응답")
	void getTeamQuestionSetStatistics_Success() throws Exception {
		// given
		Long teamId = 100L;

		QuestionSetStatisticDto dto = QuestionSetStatisticDto.builder()
			.questionSetId(10L)
			.title("실시간 퀴즈")
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.solvedAt(LocalDateTime.of(2026, 5, 31, 12, 0))
			.winners(List.of(QuestionSetWinnerDto.builder()
				.userId(5L)
				.name("위너")
				.nickname("winner")
				.build()))
			.myCorrectRate(80.0)
			.averageCorrectRate(50.0)
			.build();

		given(solvingResultService.getTeamQuestionSetStatistics(any(), eq(teamId))).willReturn(List.of(dto));

		// when & then
		mockMvc.perform(
				get("/api/v1/question-sets/statistics").param("teamId", String.valueOf(teamId)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data[0].questionSetId").value(10),
				jsonPath("$.data[0].title").value("실시간 퀴즈"),
				jsonPath("$.data[0].solveMode").value("LIVE_TIME"),
				jsonPath("$.data[0].winners[0].userId").value(5),
				jsonPath("$.data[0].winners[0].name").value("위너"),
				jsonPath("$.data[0].winners[0].nickname").value("winner"),
				jsonPath("$.data[0].myCorrectRate").value(80.0),
				jsonPath("$.data[0].averageCorrectRate").value(50.0));

		verify(solvingResultService).getTeamQuestionSetStatistics(any(), eq(teamId));
	}

	@Test
	@DisplayName("팀 문제 셋 통계 조회 - 필수 파라미터 teamId가 없으면 400")
	void getTeamQuestionSetStatistics_missingTeamId() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/question-sets/statistics"))
			.andExpect(status().isBadRequest());
	}
}
