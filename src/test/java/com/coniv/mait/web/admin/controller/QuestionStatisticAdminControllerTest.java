package com.coniv.mait.web.admin.controller;

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

import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.statistic.service.QuestionAnswerStatisticService;
import com.coniv.mait.domain.statistic.service.dto.QuestionAnswerDistributionDto;
import com.coniv.mait.domain.statistic.service.dto.SubmittedAnswerGroupDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

@WebMvcTest(controllers = QuestionStatisticAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionStatisticAdminControllerTest {

	private static final Long QUESTION_SET_ID = 1L;
	private static final Long QUESTION_ID = 5L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private QuestionAnswerStatisticService questionAnswerStatisticService;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@Test
	@DisplayName("문제별 제출 답안 분포 통계 조회 성공 - firstSubmitOnly 미지정 시 false로 호출된다")
	void getAnswerDistribution_success() throws Exception {
		// given
		given(questionAnswerStatisticService.getAnswerDistribution(eq(QUESTION_SET_ID), eq(QUESTION_ID), eq(false)))
			.willReturn(distribution());

		// when & then
		mockMvc.perform(get(
				"/api/v1/admin/question-sets/{questionSetId}/questions/{questionId}/statistics/answer-distribution",
				QUESTION_SET_ID, QUESTION_ID))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.questionSetId").value(QUESTION_SET_ID),
				jsonPath("$.data.question.id").value(QUESTION_ID),
				jsonPath("$.data.question.type").value("MULTIPLE"),
				jsonPath("$.data.totalSubmitCount").value(5),
				jsonPath("$.data.submittedUserCount").value(4),
				jsonPath("$.data.correctUserCount").value(1),
				jsonPath("$.data.correctRate").value(25.0),
				jsonPath("$.data.groups.length()").value(2),
				jsonPath("$.data.groups[0].submitCount").value(3),
				jsonPath("$.data.groups[0].isCorrect").value(false),
				jsonPath("$.data.groups[0].submittedAnswer.type").value("MULTIPLE"),
				jsonPath("$.data.groups[0].submittedAnswer.submitAnswers[0]").value(1));

		verify(questionAnswerStatisticService).getAnswerDistribution(QUESTION_SET_ID, QUESTION_ID, false);
	}

	@Test
	@DisplayName("문제별 제출 답안 분포 통계 조회 성공 - firstSubmitOnly=true 파라미터가 바인딩되어 전달된다")
	void getAnswerDistribution_firstSubmitOnly() throws Exception {
		// given
		given(questionAnswerStatisticService.getAnswerDistribution(eq(QUESTION_SET_ID), eq(QUESTION_ID), eq(true)))
			.willReturn(distribution());

		// when & then
		mockMvc.perform(get(
				"/api/v1/admin/question-sets/{questionSetId}/questions/{questionId}/statistics/answer-distribution",
				QUESTION_SET_ID, QUESTION_ID)
				.param("firstSubmitOnly", "true"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true));

		verify(questionAnswerStatisticService).getAnswerDistribution(QUESTION_SET_ID, QUESTION_ID, true);
	}

	private QuestionAnswerDistributionDto distribution() {
		return QuestionAnswerDistributionDto.builder()
			.questionSetId(QUESTION_SET_ID)
			.question(MultipleQuestionDto.builder()
				.id(QUESTION_ID)
				.content("문제 내용")
				.explanation("해설")
				.choices(List.of())
				.build())
			.totalSubmitCount(5)
			.submittedUserCount(4)
			.correctUserCount(1)
			.correctRate(25.0)
			.groups(List.of(
				SubmittedAnswerGroupDto.builder()
					.submitCount(3)
					.isCorrect(false)
					.submittedAnswer(new MultipleQuestionSubmitAnswer(List.of(1L)))
					.build(),
				SubmittedAnswerGroupDto.builder()
					.submitCount(2)
					.isCorrect(true)
					.submittedAnswer(new MultipleQuestionSubmitAnswer(List.of(1L, 2L)))
					.build()))
			.build();
	}
}
