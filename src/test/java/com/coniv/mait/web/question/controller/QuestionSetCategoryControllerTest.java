package com.coniv.mait.web.question.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.exception.QuestionSetCategoryException;
import com.coniv.mait.domain.question.exception.code.QuestionSetCategoryExceptionCode;
import com.coniv.mait.domain.question.service.QuestionSetCategoryService;
import com.coniv.mait.domain.question.service.dto.QuestionSetCategoryDto;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.question.dto.CreateQuestionSetCategoryApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = QuestionSetCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionSetCategoryControllerTest {

	private static final Long USER_ID = 10L;

	@MockitoBean
	private QuestionSetCategoryService questionSetCategoryService;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		MaitUser user = MaitUser.builder().id(USER_ID).build();
		var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
		var context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("카테고리 생성 성공 - 200 OK 와 응답 바디 반환")
	void createCategorySuccess() throws Exception {
		// given
		Long teamId = 1L;
		String name = "알고리즘";
		Long categoryId = 100L;

		CreateQuestionSetCategoryApiRequest request = new CreateQuestionSetCategoryApiRequest(teamId, name);
		QuestionSetCategoryDto dto = QuestionSetCategoryDto.builder()
			.id(categoryId)
			.teamId(teamId)
			.name(name)
			.build();

		when(questionSetCategoryService.createCategory(teamId, name, USER_ID)).thenReturn(dto);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data.id").value(categoryId),
				jsonPath("$.data.teamId").value(teamId),
				jsonPath("$.data.name").value(name));

		verify(questionSetCategoryService).createCategory(teamId, name, USER_ID);
	}

	@ParameterizedTest(name = "{index} - {0}")
	@DisplayName("카테고리 생성 실패 - 유효하지 않은 요청 (400)")
	@MethodSource("invalidCreateCategoryRequests")
	void createCategoryInvalidRequest(String testName, Long teamId, String name, String expectedReason)
		throws Exception {
		// given
		CreateQuestionSetCategoryApiRequest request = new CreateQuestionSetCategoryApiRequest(teamId, name);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.reasons[*]").value(org.hamcrest.Matchers.hasItem(expectedReason)));

		verify(questionSetCategoryService, never()).createCategory(any(), any(), any());
	}

	static Stream<Arguments> invalidCreateCategoryRequests() {
		return Stream.of(
			Arguments.of("teamId null", null, "알고리즘", "팀 정보는 필수 입니다."),
			Arguments.of("name 빈 문자열", 1L, "", "카테고리 이름을 입력해주세요."),
			Arguments.of("name null", 1L, null, "카테고리 이름을 입력해주세요."),
			Arguments.of("name 공백", 1L, "   ", "카테고리 이름을 입력해주세요."),
			Arguments.of("name 41자 초과", 1L, "가".repeat(41), "카테고리 이름은 40자 이하여야 합니다."));
	}

	@Test
	@DisplayName("카테고리 생성 실패 - 동일 이름 활성 카테고리 존재 (서비스 예외 → 409)")
	void createCategoryDuplicateActive() throws Exception {
		// given
		CreateQuestionSetCategoryApiRequest request = new CreateQuestionSetCategoryApiRequest(1L, "알고리즘");

		when(questionSetCategoryService.createCategory(anyLong(), anyString(), anyLong()))
			.thenThrow(new QuestionSetCategoryException(QuestionSetCategoryExceptionCode.DUPLICATE_NAME));

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isConflict(),
				jsonPath("$.code").value(QuestionSetCategoryExceptionCode.DUPLICATE_NAME.getCode()),
				jsonPath("$.message").value(QuestionSetCategoryExceptionCode.DUPLICATE_NAME.getMessage()));
	}

	@Test
	@DisplayName("카테고리 생성 실패 - 동일 이름 삭제된 카테고리 존재 (서비스 예외 → 409, 복구 안내)")
	void createCategoryDuplicateDeleted() throws Exception {
		// given
		CreateQuestionSetCategoryApiRequest request = new CreateQuestionSetCategoryApiRequest(1L, "알고리즘");

		when(questionSetCategoryService.createCategory(anyLong(), anyString(), anyLong()))
			.thenThrow(new QuestionSetCategoryException(QuestionSetCategoryExceptionCode.DUPLICATE_NAME_DELETED));

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isConflict(),
				jsonPath("$.code").value(QuestionSetCategoryExceptionCode.DUPLICATE_NAME_DELETED.getCode()));
	}
}
