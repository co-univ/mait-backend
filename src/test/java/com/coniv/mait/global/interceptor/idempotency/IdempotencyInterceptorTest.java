package com.coniv.mait.global.interceptor.idempotency;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.exception.CommonExceptionCode;
import com.coniv.mait.global.exception.custom.IdempotencyException;

import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class IdempotencyInterceptorTest {

	@Mock
	private IdempotencyRedisRepository repository;

	private IdempotencyInterceptor interceptor;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		interceptor = new IdempotencyInterceptor(repository);
		request = new MockHttpServletRequest("POST", "/api/v1/question-sets/1/questions/2/submit");
		request.addHeader("Idempotency-Key", "client-key");
		request.setContentType("application/json");
		request.setContent("{\"answer\":1}".getBytes(StandardCharsets.UTF_8));
		response = new MockHttpServletResponse();
		authenticate(1L);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("POST가 아니면 멱등성 저장소를 사용하지 않는다")
	void nonPostPasses() throws Exception {
		request.setMethod("GET");
		assertThat(interceptor.preHandle(request, response, null)).isTrue();
		interceptor.afterCompletion(request, response, null, null);
		verifyNoInteractions(repository);
	}

	@Test
	@DisplayName("키가 비어 있으면 멱등성 저장소를 사용하지 않는다")
	void blankKeyPasses() throws Exception {
		request.removeHeader("Idempotency-Key");
		request.addHeader("Idempotency-Key", " ");
		assertThat(interceptor.preHandle(request, response, null)).isTrue();
		interceptor.afterCompletion(request, response, null, null);
		verifyNoInteractions(repository);
	}

	@Test
	@DisplayName("인증된 사용자가 없으면 캐시를 조회하지 않는다")
	void unauthenticatedRejected() throws Exception {
		SecurityContextHolder.clearContext();
		assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
			.isInstanceOf(JwtException.class);
		verifyNoInteractions(repository);
	}

	@Test
	@DisplayName("선점에 성공한 요청만 비즈니스 처리로 진행한다")
	void claimedRequestPasses() throws Exception {
		allowClaim();
		assertThat(interceptor.preHandle(wrap(), response, null)).isTrue();
		verify(repository).claim(anyString(), any(IdempotencyRecord.class));
	}

	@Test
	@DisplayName("선점 실패 요청은 처리 중 예외를 던지고 실행 소유자의 키를 삭제하지 않는다")
	void rejectedRequestDoesNotReleaseOwner() throws Exception {
		given(repository.get(anyString())).willAnswer(invocation -> existingRequest());
		IdempotencyRequestWrapper wrapped = wrap();
		assertThatThrownBy(() -> interceptor.preHandle(wrapped, response, null))
			.isInstanceOfSatisfying(IdempotencyException.class, exception ->
				assertThat(exception.getExceptionCode()).isEqualTo(CommonExceptionCode.PROCESSING));
		interceptor.afterCompletion(wrapped, response, null, null);
		verify(repository, never()).release(anyString(), any());
	}

	@Test
	@DisplayName("같은 키로 본문을 변경하면 완료 응답을 재사용하지 않는다")
	void differentBodyRejected() throws Exception {
		given(repository.get(anyString())).willReturn(IdempotencyRecord.processing("different-body"));
		assertThatThrownBy(() -> interceptor.preHandle(wrap(), response, null))
			.isInstanceOfSatisfying(IdempotencyException.class, exception ->
					assertThat(exception.getExceptionCode()).isEqualTo(CommonExceptionCode.IDEMPOTENCY_KEY_REUSED));
	}

	@Test
	@DisplayName("캐시된 성공 응답의 상태 코드와 원문 및 헤더를 복원한다")
	void completedResponseReplayed() throws Exception {
		IdempotencyResponse cached = new IdempotencyResponse(201,
			Map.of("Content-Type", List.of("text/plain;charset=UTF-8"), "Location", List.of("/result/1")),
			"응답 원문".getBytes(StandardCharsets.UTF_8));
		given(repository.get(anyString())).willAnswer(invocation -> existingRequest().completed(cached));
		assertThat(interceptor.preHandle(wrap(), response, null)).isFalse();
		assertThat(response.getStatus()).isEqualTo(201);
		assertThat(response.getContentAsString()).isEqualTo("응답 원문");
		assertThat(response.getHeader("Location")).isEqualTo("/result/1");
		verify(repository, never()).complete(anyString(), any(), any());
	}

	@Test
	@DisplayName("사용자와 경로가 다르면 동일한 클라이언트 키도 독립적으로 선점한다")
	void userAndPathScoped() throws Exception {
		allowClaim();
		interceptor.preHandle(wrap(), response, null);
		authenticate(2L);
		interceptor.preHandle(wrap(), response, null);
		request.setRequestURI("/api/v1/question-sets/1/questions/3/submit");
		interceptor.preHandle(wrap(), response, null);
		ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
		verify(repository, times(3)).claim(keys.capture(), any());
		assertThat(keys.getAllValues()).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("성공 응답은 빈 본문이어도 완료 캐시에 저장한다")
	void successfulEmptyResponseCompleted() throws Exception {
		allowClaim();
		given(repository.complete(anyString(), any(), any())).willReturn(true);
		IdempotencyRequestWrapper wrapped = wrap();
		ContentCachingResponseWrapper cached = new ContentCachingResponseWrapper(response);
		interceptor.preHandle(wrapped, cached, null);
		cached.setStatus(204);
		interceptor.afterCompletion(wrapped, cached, null, null);
		ArgumentCaptor<IdempotencyResponse> result = ArgumentCaptor.forClass(IdempotencyResponse.class);
		verify(repository).complete(anyString(), any(), result.capture());
		assertThat(result.getValue().status()).isEqualTo(204);
		assertThat(result.getValue().body()).isEmpty();
	}

	@Test
	@DisplayName("예외가 응답으로 변환된 실패도 소유자의 키를 해제한다")
	void handledFailureReleased() throws Exception {
		allowClaim();
		IdempotencyRequestWrapper wrapped = wrap();
		interceptor.preHandle(wrapped, response, null);
		response.setStatus(400);
		interceptor.afterCompletion(wrapped, response, null, null);
		verify(repository).release(anyString(), any());
		verify(repository, never()).complete(anyString(), any(), any());
	}

	@Test
	@DisplayName("처리되지 않은 예외가 있으면 상태 코드가 200이어도 키를 해제한다")
	void unhandledFailureReleased() throws Exception {
		allowClaim();
		IdempotencyRequestWrapper wrapped = wrap();
		interceptor.preHandle(wrapped, response, null);
		interceptor.afterCompletion(wrapped, response, null, new IllegalStateException("rollback"));
		verify(repository).release(anyString(), any());
		verify(repository, never()).complete(anyString(), any(), any());
	}

	@Test
	@DisplayName("커밋 후 캐시 저장 실패는 실행 실패로 취급해 선점 키를 삭제하지 않는다")
	void cacheFailureDoesNotReleaseSuccessfulExecution() throws Exception {
		allowClaim();
		willThrow(new IllegalStateException("Redis unavailable"))
			.given(repository).complete(anyString(), any(), any());
		IdempotencyRequestWrapper wrapped = wrap();
		ContentCachingResponseWrapper cached = new ContentCachingResponseWrapper(response);
		interceptor.preHandle(wrapped, cached, null);
		assertThatThrownBy(() -> interceptor.afterCompletion(wrapped, cached, null, null))
			.isInstanceOf(IllegalStateException.class).hasMessage("Redis unavailable");
		verify(repository, never()).release(anyString(), any());
	}

	@Test
	@DisplayName("Redis 선점 오류 시 비즈니스 처리를 허용하지 않는다")
	void claimFailureDoesNotExecute() throws Exception {
		given(repository.claim(anyString(), any())).willThrow(new IllegalStateException("Redis unavailable"));
		IdempotencyRequestWrapper wrapped = wrap();
		assertThatThrownBy(() -> interceptor.preHandle(wrapped, response, null))
			.isInstanceOf(IllegalStateException.class).hasMessage("Redis unavailable");
		interceptor.afterCompletion(wrapped, response, null, null);
		verify(repository, never()).release(anyString(), any());
	}

	private void allowClaim() {
		given(repository.claim(anyString(), any())).willReturn(true);
	}

	private IdempotencyRecord existingRequest() {
		ArgumentCaptor<IdempotencyRecord> record = ArgumentCaptor.forClass(IdempotencyRecord.class);
		verify(repository).claim(anyString(), record.capture());
		return record.getValue();
	}

	private IdempotencyRequestWrapper wrap() throws Exception {
		return new IdempotencyRequestWrapper(request);
	}

	private void authenticate(Long userId) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
			MaitUser.builder().id(userId).build(), "", List.of()));
	}
}
