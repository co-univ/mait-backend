package com.coniv.mait.global.interceptor.idempotency;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class IdempotencyInterceptorTest {

	@Mock
	IdempotencyRedisRepository redisRepository;

	@Mock
	ObjectMapper objectMapper;

	@Mock
	HttpServletRequest request;

	@Mock
	HttpServletResponse response;

	@Mock
	HandlerMethod handler;

	@InjectMocks
	IdempotencyInterceptor interceptor;

	@Test
	@DisplayName("POST가 아니면 무조건 통과")
	void notPost_requestPasses() throws Exception {
		given(request.getMethod()).willReturn("GET");

		boolean result = interceptor.preHandle(request, response, handler);

		assertThat(result).isTrue();
		verifyNoInteractions(redisRepository);
	}

	@Test
	@DisplayName("헤더 없으면 무조건 통과")
	void noHeader_requestPasses() throws Exception {
		given(request.getMethod()).willReturn("POST");
		given(request.getHeader("Idempotency-Key")).willReturn(null);

		boolean result = interceptor.preHandle(request, response, handler);

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("처리중이면 409 반환")
	void processing_conflict() throws Exception {
		given(request.getMethod()).willReturn("POST");
		given(request.getHeader("Idempotency-Key")).willReturn("abc123");
		given(redisRepository.getStatus("abc123")).willReturn(IdempotencyStatus.PROCESSING);

		given(objectMapper.writeValueAsString(any()))
			.willReturn("{\"error\":\"PROCESSING\"}");

		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		given(response.getWriter()).willReturn(writer);

		boolean result = interceptor.preHandle(request, response, handler);

		assertThat(result).isFalse();
		assertThat(stringWriter.toString()).contains("PROCESSING");
	}

	@Test
	@DisplayName("처음 요청이면 PROCESSING 저장")
	void firstRequest_setProcessing() throws Exception {
		given(request.getMethod()).willReturn("POST");
		given(request.getHeader("Idempotency-Key")).willReturn("abc123");
		given(redisRepository.getStatus("abc123")).willReturn(null);

		boolean result = interceptor.preHandle(request, response, handler);

		assertThat(result).isTrue();
		verify(redisRepository).setProcessing("abc123");
	}
}
