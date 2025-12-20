package com.coniv.mait.web.question.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.service.QuestionReviewService;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.question.dto.LastViewedQuestionApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = QuestionReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionReviewControllerTest {

	private static final Long USER_ID = 2L;

	@MockitoBean
	private QuestionReviewService questionReviewService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	private Authentication authentication;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		UserEntity user = mock(UserEntity.class);
		when(user.getId()).thenReturn(USER_ID);
		authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());

		// addFilters = false 환경에서는 @AuthenticationPrincipal 주입이 null이 될 수 있어서
		// 테스트에서 SecurityContextHolder를 직접 세팅해준다.
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("마지막 조회한 문제 조회 컨트롤러 테스트")
	void getLastViewedQuestionInReview_success() throws Exception {
		// given
		final Long questionSetId = 1L;

		MultipleQuestionDto questionDto = MultipleQuestionDto.builder()
			.id(10L)
			.content("객관식 문제 내용")
			.explanation("문제 해설")
			.number(1L)
			.choices(List.of(
				MultipleChoiceDto.builder().id(100L).number(1).content("선택지1").isCorrect(true).build(),
				MultipleChoiceDto.builder().id(101L).number(2).content("선택지2").isCorrect(false).build()))
			.build();

		when(questionReviewService.getLastViewedQuestionInReview(questionSetId, USER_ID)).thenReturn(questionDto);

		// when
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/last-viewed", questionSetId)
				.with(authentication(authentication)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").exists(),
				jsonPath("$.data.id").value(10L),
				jsonPath("$.data.type").value("MULTIPLE"),
				jsonPath("$.data.content").value("객관식 문제 내용"),
				jsonPath("$.data.number").value(1L),
				jsonPath("$.data.choices").isArray(),
				jsonPath("$.data.choices.length()").value(2),
				jsonPath("$.data.choices[0].id").value(100L),
				jsonPath("$.data.choices[0].number").value(1),
				jsonPath("$.data.choices[0].content").value("선택지1"),
				jsonPath("$.data.choices[0].isCorrect").value(true));

		// then
		verify(questionReviewService).getLastViewedQuestionInReview(questionSetId, USER_ID);
	}

	@Test
	@DisplayName("마지막 조회한 문제 업데이트 - 성공")
	void updateLastViewedQuestion_success() throws Exception {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 10L;

		LastViewedQuestionApiRequest request = new LastViewedQuestionApiRequest(questionId);
		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/last-viewed", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json)
				.with(authentication(authentication)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").doesNotExist());

		// then
		verify(questionReviewService).updateLastViewedQuestion(questionSetId, questionId, USER_ID);
	}

	@Test
	@DisplayName("마지막 조회한 문제 업데이트 - 실패 (questionId null로 validation 실패)")
	void updateLastViewedQuestion_validationFail_questionIdNull() throws Exception {
		// given
		final Long questionSetId = 1L;
		LastViewedQuestionApiRequest request = new LastViewedQuestionApiRequest(null);
		String json = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/last-viewed", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json)
				.with(authentication(authentication)))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons[0]").value("마지막으로 조회한 문제 PK를 입력해주세요"));

		verify(questionReviewService, never()).updateLastViewedQuestion(anyLong(), anyLong(), anyLong());
	}
}
