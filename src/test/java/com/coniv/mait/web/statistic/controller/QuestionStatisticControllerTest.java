package com.coniv.mait.web.statistic.controller;

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

import com.coniv.mait.domain.statistic.service.QuestionStatisticService;
import com.coniv.mait.domain.statistic.service.dto.QuestionStatisticDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

@WebMvcTest(controllers = QuestionStatisticController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionStatisticControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private QuestionStatisticService questionStatisticService;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@Test
	@DisplayName("문제 셋 문제별 오답률 조회 성공")
	void getWrongRates_Success() throws Exception {
		// given
		Long questionSetId = 1L;

		List<QuestionStatisticDto> statistics = List.of(
			QuestionStatisticDto.builder()
				.questionId(102L).questionNumber(2L)
				.submittedUserCount(2).firstWrongUserCount(2).wrongRate(100.0)
				.build(),
			QuestionStatisticDto.builder()
				.questionId(101L).questionNumber(1L)
				.submittedUserCount(4).firstWrongUserCount(1).wrongRate(25.0)
				.build());

		given(questionStatisticService.getWrongRates(eq(questionSetId), any())).willReturn(statistics);

		// when & then
		mockMvc.perform(
				get("/api/v1/question-sets/{questionSetId}/questions/wrong-rates", questionSetId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(2),
				jsonPath("$.data[0].questionId").value(102),
				jsonPath("$.data[0].questionNumber").value(2),
				jsonPath("$.data[0].submittedUserCount").value(2),
				jsonPath("$.data[0].firstWrongUserCount").value(2),
				jsonPath("$.data[0].wrongRate").value(100.0),
				jsonPath("$.data[1].questionId").value(101),
				jsonPath("$.data[1].wrongRate").value(25.0));

		verify(questionStatisticService).getWrongRates(eq(questionSetId), any());
	}
}
