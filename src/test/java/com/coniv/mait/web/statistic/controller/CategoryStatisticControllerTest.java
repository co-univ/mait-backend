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

import com.coniv.mait.domain.statistic.service.CategoryStatisticService;
import com.coniv.mait.domain.statistic.service.dto.CategoryCorrectRateDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

@WebMvcTest(controllers = CategoryStatisticController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryStatisticControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CategoryStatisticService categoryStatisticService;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@Test
	@DisplayName("카테고리별 정답률 단건 조회 성공")
	void getCategoryCorrectRate_Success() throws Exception {
		// given
		Long teamId = 1L;
		Long categoryId = 10L;
		CategoryCorrectRateDto dto = CategoryCorrectRateDto.builder()
			.categoryId(categoryId).categoryName("Java")
			.questionSetCount(2).myCorrectRate(80.0).averageCorrectRate(70.0)
			.build();
		given(categoryStatisticService.getCategoryCorrectRate(any(), eq(teamId), eq(categoryId))).willReturn(dto);

		// when & then
		mockMvc.perform(
				get("/api/v1/teams/{teamId}/categories/{categoryId}/correct-rate", teamId, categoryId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.categoryId").value(categoryId),
				jsonPath("$.data.categoryName").value("Java"),
				jsonPath("$.data.questionSetCount").value(2),
				jsonPath("$.data.myCorrectRate").value(80.0),
				jsonPath("$.data.averageCorrectRate").value(70.0));
	}

	@Test
	@DisplayName("팀 카테고리별 정답률 랭킹 조회 성공")
	void getCategoryCorrectRates_Success() throws Exception {
		// given
		Long teamId = 1L;
		List<CategoryCorrectRateDto> ranking = List.of(
			CategoryCorrectRateDto.builder()
				.categoryId(2L).categoryName("Spring")
				.questionSetCount(1).myCorrectRate(90.0).averageCorrectRate(60.0)
				.build(),
			CategoryCorrectRateDto.builder()
				.categoryId(3L).categoryName("DB")
				.questionSetCount(0).myCorrectRate(null).averageCorrectRate(0.0)
				.build());
		given(categoryStatisticService.getCategoryCorrectRates(any(), eq(teamId))).willReturn(ranking);

		// when & then
		mockMvc.perform(
				get("/api/v1/teams/{teamId}/categories/correct-rates", teamId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(2),
				jsonPath("$.data[0].categoryId").value(2),
				jsonPath("$.data[0].myCorrectRate").value(90.0),
				jsonPath("$.data[1].categoryId").value(3),
				jsonPath("$.data[1].myCorrectRate").doesNotExist());
	}
}
