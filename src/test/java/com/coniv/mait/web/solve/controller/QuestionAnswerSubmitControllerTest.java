package com.coniv.mait.web.solve.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.solve.service.QuestionAnswerSubmitService;
import com.coniv.mait.domain.solve.service.QuestionScorerService;
import com.coniv.mait.domain.solve.service.dto.AnswerSubmitDto;
import com.coniv.mait.domain.solve.service.dto.QuestionScorerDto;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = QuestionAnswerSubmitController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionAnswerSubmitControllerTest {
	// todo: 디테일한 테스트 추가 필요

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private QuestionAnswerSubmitService questionAnswerSubmitService;

	@MockitoBean
	private QuestionScorerService questionScorerService;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);
	}

	@Test
	@DisplayName("객관식 문제 정답 제출 성공")
	void submitMultipleQuestionAnswer_Success() throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long userId = 1L;

		String requestJson = """
			{
				"type": "MULTIPLE",
				"userId": %d,
				"submitAnswers": [1, 2]
			}
			""".formatted(userId);

		AnswerSubmitDto mockResponse = AnswerSubmitDto.builder()
			.id(1L)
			.userId(userId)
			.questionId(questionId)
			.isCorrect(true)
			.build();

		// When & Then
		when(questionAnswerSubmitService.submitAnswer(eq(questionSetId), eq(questionId), eq(userId), any()))
			.thenReturn(mockResponse);

		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.userId").value(userId))
			.andExpect(jsonPath("$.data.questionId").value(questionId))
			.andExpect(jsonPath("$.data.isCorrect").value(true));
	}

	@Test
	@DisplayName("주관식 문제 정답 제출 성공")
	void submitShortQuestionAnswer_Success() throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 2L;
		Long userId = 1L;

		String requestJson = """
			{
				"type": "SHORT",
				"userId": %d,
				"submitAnswers": ["정답입니다"]
			}
			""".formatted(userId);

		AnswerSubmitDto mockResponse = AnswerSubmitDto.builder()
			.id(2L)
			.userId(userId)
			.questionId(questionId)
			.isCorrect(false)
			.build();

		// When & Then
		when(questionAnswerSubmitService.submitAnswer(eq(questionSetId), eq(questionId), eq(userId), any()))
			.thenReturn(mockResponse);

		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.id").value(2))
			.andExpect(jsonPath("$.data.userId").value(userId))
			.andExpect(jsonPath("$.data.questionId").value(questionId))
			.andExpect(jsonPath("$.data.isCorrect").value(false));
	}

	@Test
	@DisplayName("userId가 null인 경우 validation 실패")
	void submitAnswer_ValidationFailed_UserIdNull() throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;

		String requestJson = """
			{
				"type": "MULTIPLE",
				"userId": null,
				"submitAnswers": [1]
			}
			""";

		// When & Then
		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.reasons[0]").value("유저 ID를 입력해주세요."));
	}

	@Test
	@DisplayName("객관식 문제에서 submitAnswers가 null인 경우 validation 실패")
	void submitMultipleQuestionAnswer_ValidationFailed_SubmitAnswersNull() throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long userId = 1L;

		String requestJson = """
			{
				"type": "MULTIPLE",
				"userId": %d,
				"submitAnswers": null
			}
			""".formatted(userId);

		// When & Then
		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.reasons[0]").value("객관식 문제의 선택지는 필수입니다."));
	}

	@Test
	@DisplayName("객관식 문제에서 submitAnswers가 빈 리스트인 경우 validation 실패")
	void submitMultipleQuestionAnswer_ValidationFailed_SubmitAnswersEmpty() throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long userId = 1L;

		String requestJson = """
			{
				"type": "MULTIPLE",
				"userId": %d,
				"submitAnswers": []
			}
			""".formatted(userId);

		// When & Then
		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.reasons[0]").value("객관식 문제는 최소 1개의 선택지를 선택해야 합니다."));
	}

	@Test
	@DisplayName("주관식 문제에서 submitAnswers가 null인 경우 validation 실패")
	void submitShortQuestionAnswer_ValidationFailed_SubmitAnswersNull() throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 2L;
		Long userId = 1L;

		String requestJson = """
			{
				"type": "SHORT",
				"userId": %d,
				"submitAnswers": null
			}
			""".formatted(userId);

		// When & Then
		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.reasons[0]").value("주관식 문제의 답변은 필수입니다."));
	}

	@Test
	@DisplayName("주관식 문제에서 submitAnswers가 빈 리스트인 경우 validation 실패")
	void submitShortQuestionAnswer_ValidationFailed_SubmitAnswersEmpty() throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 2L;
		Long userId = 1L;

		String requestJson = """
			{
				"type": "SHORT",
				"userId": %d,
				"submitAnswers": []
			}
			""".formatted(userId);

		// When & Then
		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.reasons[0]").value("주관식 문제는 최소 1글자 이상의 답변이 필요합니다."));
	}

	@Test
	@DisplayName("잘못된 JSON 형식으로 요청 시 validation 실패")
	void submitAnswer_ValidationFailed_InvalidJson() throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;

		String invalidJson = "{\"userId\": 1, \"submitAnswers\": [1, 2], \"invalidField\": \"value\"}";

		// When & Then
		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(invalidJson))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("문제별 득점자 조회 성공")
	void getScorer_Success() throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long userId = 1L;

		QuestionScorerDto mockResponse = QuestionScorerDto.builder()
			.id(1L)
			.questionId(questionId)
			.userId(userId)
			.userName("테스트사용자")
			.submitOrder(1L)
			.build();

		// When & Then
		when(questionScorerService.getScorer(questionSetId, questionId))
			.thenReturn(mockResponse);

		mockMvc.perform(
				get("/api/v1/question-sets/{questionSetId}/questions/{questionId}/scorer", questionSetId, questionId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.questionId").value(questionId))
			.andExpect(jsonPath("$.data.userId").value(userId))
			.andExpect(jsonPath("$.data.userName").value("테스트사용자"))
			.andExpect(jsonPath("$.data.submitOrder").value(1));

		verify(questionScorerService).getScorer(questionSetId, questionId);
	}
}
