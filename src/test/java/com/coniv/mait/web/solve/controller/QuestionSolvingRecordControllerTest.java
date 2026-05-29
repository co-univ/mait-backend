package com.coniv.mait.web.solve.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.solve.service.SolvingResultService;
import com.coniv.mait.domain.solve.service.dto.MySolveRecordDto;
import com.coniv.mait.domain.solve.service.dto.QuestionSolveResultDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

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
}
