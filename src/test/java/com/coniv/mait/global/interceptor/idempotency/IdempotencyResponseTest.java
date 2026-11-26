package com.coniv.mait.global.interceptor.idempotency;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

@ExtendWith(MockitoExtension.class)
class IdempotencyResponseTest {

	@Mock
	private ContentCachingResponseWrapper response;

	@Test
	@DisplayName("헤더 목록이 비어 있어도 응답의 Content-Type을 캐시에 보존한다")
	void storesContentTypeFromResponse() {
		// given
		given(response.getHeaders("Content-Type")).willReturn(List.of());
		given(response.getContentType()).willReturn("application/json;charset=UTF-8");
		given(response.getStatus()).willReturn(200);
		given(response.getContentAsByteArray()).willReturn("{\"result\":true}".getBytes(StandardCharsets.UTF_8));

		// when
		IdempotencyResponse cached = IdempotencyResponse.from(response);

		// then
		assertThat(cached.headers()).containsEntry("Content-Type", List.of("application/json;charset=UTF-8"));
		assertThat(cached.status()).isEqualTo(200);
		assertThat(cached.body()).isEqualTo("{\"result\":true}".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	@DisplayName("헤더 존재 여부와 달리 값이 비어 있으면 캐시에 저장하지 않는다")
	void doesNotStoreEmptyHeaderValues() {
		// given
		given(response.getHeaders(anyString())).willReturn(List.of());

		// when
		IdempotencyResponse cached = IdempotencyResponse.from(response);

		// then
		assertThat(cached.headers()).doesNotContainKey("Location");
	}

	@Test
	@DisplayName("이미 저장된 빈 헤더는 건너뛰고 상태와 본문 및 나머지 헤더를 복원한다")
	void replaysLegacyEmptyHeaderWithoutException() throws Exception {
		// given
		IdempotencyResponse cached = new IdempotencyResponse(201,
			Map.of("Content-Type", List.of(), "Location", List.of("/result/1")),
			"응답 원문".getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse target = new MockHttpServletResponse();

		// when
		cached.writeTo(target);

		// then
		assertThat(target.getStatus()).isEqualTo(201);
		assertThat(target.getContentAsByteArray()).isEqualTo(cached.body());
		assertThat(target.getHeader("Location")).isEqualTo("/result/1");
	}
}
