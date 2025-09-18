package com.coniv.mait.web.question.controller;

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

import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.service.QuestionSetService;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.question.dto.CreateQuestionSetApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = QuestionSetController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionSetControllerTest {

	@MockitoBean
	private QuestionSetService questionSetService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);
	}

	@Test
	@DisplayName("문제 셋 생성 테스트")
	void createQuestionSetTest() throws Exception {
		// given
		final Long questionSetId = 1L;
		String subject = "Sample Subject";
		QuestionSetCreationType creationType = QuestionSetCreationType.MANUAL;
		CreateQuestionSetApiRequest request = new CreateQuestionSetApiRequest(subject,
			creationType);

		QuestionSetDto questionSetDto = QuestionSetDto.builder()
			.id(questionSetId)
			.subject(subject)
			.build();

		when(questionSetService.createQuestionSet(subject, creationType)).thenReturn(questionSetDto);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.questionSetId").value(questionSetId))
			.andExpect(jsonPath("$.data.subject").value(subject));

		// then
		verify(questionSetService).createQuestionSet(request.subject(), request.creationType());
	}

	@ParameterizedTest(name = "{index} - {0}")
	@DisplayName("문제 셋 생성 실패 테스트 - 유효하지 않은 요청")
	@MethodSource("invalidCreateQuestionSetRequests")
	void createQuestionSetInvalidRequestTest(String testName, String subject, QuestionSetCreationType creationType,
		String expectedMessage) throws Exception {
		// given
		CreateQuestionSetApiRequest request = new CreateQuestionSetApiRequest(subject, creationType);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons[0]").value(expectedMessage)
			);

		verify(questionSetService, never()).createQuestionSet(anyString(), any());
	}

	static Stream<Arguments> invalidCreateQuestionSetRequests() {
		return Stream.of(
			Arguments.of("빈 제목", "", QuestionSetCreationType.MANUAL, "교육 주제를 입력해주세요."),
			Arguments.of("null 제목", null, QuestionSetCreationType.MANUAL, "교육 주제를 입력해주세요."),
			Arguments.of("공백만 있는 제목", "   ", QuestionSetCreationType.MANUAL, "교육 주제를 입력해주세요."),
			Arguments.of("빈 생성 타입", "Valid Subject", null, "문제 셋 생성 유형을 선택해주세요.")
		);
	}

	@Test
	@DisplayName("문제 셋 생성 실패 테스트 - 여러 필드 동시 유효성 검증 실패")
	void createQuestionSetMultipleValidationFailuresTest() throws Exception {
		// given
		CreateQuestionSetApiRequest request = new CreateQuestionSetApiRequest(null, null);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons.length()").value(2),
				jsonPath("$.reasons[*]").value(org.hamcrest.Matchers.hasItems(
					"교육 주제를 입력해주세요.",
					"문제 셋 생성 유형을 선택해주세요."
				))
			);

		verify(questionSetService, never()).createQuestionSet(anyString(), any());
	}

	@Test
	@DisplayName("문제 셋 목록 조회 테스트")
	void getQuestionSetsTest() throws Exception {
		// given
		Long teamId = 1L;
		QuestionSetDto questionSet1 = QuestionSetDto.builder().id(1L).subject("Subject 1").build();
		QuestionSetDto questionSet2 = QuestionSetDto.builder().id(2L).subject("Subject 2").build();
		when(questionSetService.getQuestionSets(teamId, null)).thenReturn(List.of(questionSet1, questionSet2));

		// when & then
		mockMvc.perform(get("/api/v1/question-sets")
				.param("teamId", String.valueOf(teamId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].id").value(1L))
			.andExpect(jsonPath("$.data[0].subject").value("Subject 1"))
			.andExpect(jsonPath("$.data[1].id").value(2L))
			.andExpect(jsonPath("$.data[1].subject").value("Subject 2"));

		verify(questionSetService).getQuestionSets(teamId, null);
	}

	@Test
	@DisplayName("문제 셋 단건 조회 테스트")
	void getQuestionSetTest() throws Exception {
		// given
		final Long questionSetId = 1L;
		final String subject = "Test Subject";
		QuestionSetDto questionSetDto = QuestionSetDto.builder()
			.id(questionSetId)
			.subject(subject)
			.build();

		when(questionSetService.getQuestionSet(questionSetId)).thenReturn(questionSetDto);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}", questionSetId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(questionSetId))
			.andExpect(jsonPath("$.data.subject").value(subject));

		verify(questionSetService).getQuestionSet(questionSetId);
	}
}
