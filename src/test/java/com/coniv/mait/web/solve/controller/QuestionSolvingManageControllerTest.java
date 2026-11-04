package com.coniv.mait.web.solve.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.solve.service.QuestionSolvingManageService;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

@WebMvcTest(controllers = QuestionSolvingManageController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionSolvingManageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private QuestionSolvingManageService service;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@ParameterizedTest
	@CsvSource({"SHORT, shortAnswers", "FILL_BLANK, answers"})
	@DisplayName("메인 정답이 아닌 유효한 인정답안 추가 요청은 통과한다")
	void addAnswer_validRequest(String type, String field) throws Exception {
		// given
		String body = """
			{"payload":{"type":"%s","%s":[{"id":-1,"number":1,"answer":"gamma","main":false}]}}
			""".formatted(type, field);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/1/questions/2/answers")
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpectAll(status().isOk(), jsonPath("$.isSuccess").value(true));
		verify(service).updateQuestionAnswers(eq(1L), eq(2L),
			argThat(payload -> payload.getType().name().equals(type)));
	}

	@ParameterizedTest
	@MethodSource("invalidRequests")
	@DisplayName("메인·빈 답안·누락된 번호·빈 목록 요청은 DTO 검증에서 거절한다")
	void addAnswer_invalidRequest(String body) throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/question-sets/1/questions/2/answers")
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpectAll(status().isBadRequest(), jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"));
		verifyNoInteractions(service);
	}

	static Stream<String> invalidRequests() {
		return Stream.of("SHORT", "FILL_BLANK").flatMap(type -> {
			String field = "answers";
			if ("SHORT".equals(type)) {
				field = "shortAnswers";
			}
			String template = "{\"payload\":{\"type\":\"" + type + "\",\"" + field + "\":%s}}";
			return Stream.of(
				"[{\"number\":1,\"answer\":\"gamma\",\"main\":true}]",
				"[{\"number\":1,\"answer\":\"\",\"main\":false}]",
				"[{\"number\":1,\"answer\":\"   \",\"main\":false}]",
				"[{\"number\":1,\"answer\":null,\"main\":false}]",
				"[{\"number\":null,\"answer\":\"gamma\",\"main\":false}]",
				"[null]", "[]", "null"
			).map(template::formatted);
		});
	}
}
