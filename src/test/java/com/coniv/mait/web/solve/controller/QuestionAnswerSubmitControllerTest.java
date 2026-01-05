package com.coniv.mait.web.solve.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.solve.service.QuestionAnswerSubmitService;
import com.coniv.mait.domain.solve.service.QuestionScorerService;
import com.coniv.mait.domain.solve.service.dto.AnswerSubmitDto;
import com.coniv.mait.domain.solve.service.dto.AnswerSubmitRecordDto;
import com.coniv.mait.domain.solve.service.dto.QuestionScorerDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
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

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

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
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.id").value(1),
				jsonPath("$.data.userId").value(userId),
				jsonPath("$.data.questionId").value(questionId),
				jsonPath("$.data.isCorrect").value(true));
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
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.id").value(2),
				jsonPath("$.data.userId").value(userId),
				jsonPath("$.data.questionId").value(questionId),
				jsonPath("$.data.isCorrect").value(false));
	}

	static Stream<Arguments> validationFailureCases() {
		return Stream.of(
			Arguments.of(
				"userId가 null인 경우",
				"""
					{
						"type": "MULTIPLE",
						"userId": null,
						"submitAnswers": [1]
					}
					""",
				"유저 ID를 입력해주세요."),
			Arguments.of(
				"객관식 문제에서 submitAnswers가 null인 경우",
				"""
					{
						"type": "MULTIPLE",
						"userId": 1,
						"submitAnswers": null
					}
					""",
				"객관식 문제의 선택지는 필수입니다."),
			Arguments.of(
				"객관식 문제에서 submitAnswers가 빈 리스트인 경우",
				"""
					{
						"type": "MULTIPLE",
						"userId": 1,
						"submitAnswers": []
					}
					""",
				"객관식 문제는 최소 1개의 선택지를 선택해야 합니다."),
			Arguments.of(
				"주관식 문제에서 submitAnswers가 null인 경우",
				"""
					{
						"type": "SHORT",
						"userId": 1,
						"submitAnswers": null
					}
					""",
				"주관식 문제의 답변은 필수입니다."),
			Arguments.of(
				"주관식 문제에서 submitAnswers가 빈 리스트인 경우",
				"""
					{
						"type": "SHORT",
						"userId": 1,
						"submitAnswers": []
					}
					""",
				"주관식 문제는 최소 1개 이상의 답변이 필요합니다."),
			Arguments.of(
				"주관식 문제에서 submitAnswers에 빈 문자열이 포함된 경우",
				"""
					{
						"type": "SHORT",
						"userId": 1,
						"submitAnswers": [""]
					}
					""",
				"답변은 빈 문자열일 수 없습니다."));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("validationFailureCases")
	@DisplayName("정답 제출 API validation 실패 테스트")
	void submitAnswer_ValidationFailed(String testName, String requestJson, String expectedMessage) throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;

		// When & Then
		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestJson))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.reasons[0]").value(expectedMessage));
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
				get("/api/v1/question-sets/{questionSetId}/questions/{questionId}/scorers", questionSetId, questionId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.id").value(1),
				jsonPath("$.data.questionId").value(questionId),
				jsonPath("$.data.userId").value(userId),
				jsonPath("$.data.userName").value("테스트사용자"),
				jsonPath("$.data.submitOrder").value(1));

		verify(questionScorerService).getScorer(questionSetId, questionId);
	}

	@Test
	@DisplayName("문제 풀이 정답 제출 기록 조회 API 성공 테스트")
	void getSubmitRecords_Success() throws Exception {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long userId1 = 1L;
		Long userId2 = 2L;

		List<AnswerSubmitRecordDto> mockRecords = List.of(
			AnswerSubmitRecordDto.builder()
				.id(1L)
				.userId(userId1)
				.userName("사용자1")
				.questionId(questionId)
				.isCorrect(true)
				.submitOrder(1L)
				.build(),
			AnswerSubmitRecordDto.builder()
				.id(2L)
				.userId(userId2)
				.userName("사용자2")
				.questionId(questionId)
				.isCorrect(false)
				.submitOrder(2L)
				.build());

		// When & Then
		when(questionAnswerSubmitService.getSubmitRecords(questionSetId, questionId))
			.thenReturn(mockRecords);

		mockMvc.perform(
				get("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit-records",
					questionSetId, questionId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.totalCounts").value(2),
				jsonPath("$.data.submitRecords[0].id").value(1),
				jsonPath("$.data.submitRecords[0].userId").value(userId1),
				jsonPath("$.data.submitRecords[0].userName").value("사용자1"),
				jsonPath("$.data.submitRecords[0].questionId").value(questionId),
				jsonPath("$.data.submitRecords[0].isCorrect").value(true),
				jsonPath("$.data.submitRecords[0].submitOrder").value(1),
				jsonPath("$.data.submitRecords[1].id").value(2),
				jsonPath("$.data.submitRecords[1].userId").value(userId2),
				jsonPath("$.data.submitRecords[1].userName").value("사용자2"),
				jsonPath("$.data.submitRecords[1].questionId").value(questionId),
				jsonPath("$.data.submitRecords[1].isCorrect").value(false),
				jsonPath("$.data.submitRecords[1].submitOrder").value(2));

		verify(questionAnswerSubmitService).getSubmitRecords(questionSetId, questionId);
	}
}
