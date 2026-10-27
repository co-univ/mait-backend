package com.coniv.mait.global.filter;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.coniv.mait.global.interceptor.idempotency.IdempotencyRequestWrapper;

import jakarta.servlet.ServletException;

class ContentWrappingFilterTest {

	private final ContentWrappingFilter filter = new ContentWrappingFilter();

	@Test
	@DisplayName("멱등성 대상 본문은 해시 계산 후에도 컨트롤러에서 원문을 읽을 수 있다")
	void protectedRequestPreservesBody() throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("POST",
			"/api/v1/question-sets/1/questions/2/submit");
		request.addHeader("Idempotency-Key", "key");
		request.setCharacterEncoding("UTF-8");
		byte[] body = "{\"answer\":\"답안\"}".getBytes(StandardCharsets.UTF_8);
		request.setContent(body);
		MockHttpServletResponse response = new MockHttpServletResponse();
		// when
		filter.doFilter(request, response, (wrapped, cached) -> {
			assertThat(wrapped).isInstanceOf(IdempotencyRequestWrapper.class);
			assertThat(((IdempotencyRequestWrapper)wrapped).getBody()).isEqualTo(body);
			assertThat(wrapped.getInputStream().readAllBytes()).isEqualTo(body);
			assertThat(wrapped.getReader().readLine()).isEqualTo(new String(body, StandardCharsets.UTF_8));
			cached.getOutputStream().write(body);
		});
		// then
		assertThat(response.getContentAsByteArray()).isEqualTo(body);
	}

	@Test
	@DisplayName("대상이 아닌 경로는 키가 있어도 원래 요청을 전달한다")
	void unrelatedPathPassesOriginalRequest() throws Exception {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
		request.addHeader("Idempotency-Key", "key");
		// when / then
		filter.doFilter(request, new MockHttpServletResponse(), (wrapped, response) ->
			assertThat(wrapped).isSameAs(request));
	}

	@Test
	@DisplayName("처리되지 않은 예외가 발생하면 부분 응답을 전송해 정상 응답으로 확정하지 않는다")
	void unhandledExceptionDoesNotCopyPartialBody() {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("POST",
			"/api/v1/question-sets/1/questions/2/submit");
		MockHttpServletResponse response = new MockHttpServletResponse();
		// when / then
		assertThatThrownBy(() -> filter.doFilter(request, response, (wrapped, cached) -> {
			cached.getWriter().write("partial response");
			throw new ServletException("failed");
		})).isInstanceOf(ServletException.class).hasMessage("failed");
		assertThat(response.isCommitted()).isFalse();
		assertThat(response.getContentAsByteArray()).isEmpty();
	}
}
