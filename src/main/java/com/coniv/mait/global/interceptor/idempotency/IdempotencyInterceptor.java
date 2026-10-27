package com.coniv.mait.global.interceptor.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.exception.CommonExceptionCode;
import com.coniv.mait.global.exception.custom.IdempotencyException;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

	public static final String PATH_PATTERN = "/api/v1/question-sets/*/questions/*/submit";
	public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

	private static final String EXECUTION_ATTRIBUTE = IdempotencyInterceptor.class.getName() + ".execution";
	private final IdempotencyRedisRepository idempotencyRedisRepository;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws
		Exception {
		String clientKey = request.getHeader(IDEMPOTENCY_HEADER);
		if (!HttpMethod.POST.name().equals(request.getMethod()) || clientKey == null || clientKey.isBlank()) {
			return true;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
			|| !(authentication.getPrincipal() instanceof MaitUser user)) {
			throw new JwtException(CommonExceptionCode.JWT_AUTH_EXCEPTION.getMessage());
		}

		IdempotencyRequestWrapper wrapped = WebUtils.getNativeRequest(request, IdempotencyRequestWrapper.class);
		if (wrapped == null) {
			throw new IllegalStateException("멱등성 요청 본문 래퍼가 필요합니다.");
		}
		String path = request.getRequestURI().substring(request.getContextPath().length());
		String key = user.id() + ":" + hash(path) + ":" + hash(clientKey);
		String fingerprint = hash(wrapped.getBody());
		IdempotencyRecord processing = IdempotencyRecord.processing(fingerprint);
		if (idempotencyRedisRepository.claim(key, processing)) {
			request.setAttribute(EXECUTION_ATTRIBUTE, new Execution(key, processing));
			return true;
		}

		IdempotencyRecord existing = idempotencyRedisRepository.get(key);
		if (existing != null && !existing.fingerprint().equals(fingerprint)) {
			throw new IdempotencyException(CommonExceptionCode.IDEMPOTENCY_KEY_REUSED);
		}
		if (existing != null && existing.status() == IdempotencyStatus.COMPLETED) {
			existing.response().writeTo(response);
			return false;
		}
		throw new IdempotencyException(CommonExceptionCode.PROCESSING);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
		Exception exception) {
		Execution execution = (Execution)request.getAttribute(EXECUTION_ATTRIBUTE);
		if (execution == null) {
			return;
		}
		request.removeAttribute(EXECUTION_ATTRIBUTE);
		ContentCachingResponseWrapper wrapped = WebUtils.getNativeResponse(response,
			ContentCachingResponseWrapper.class);
		boolean success = exception == null && HttpStatusCode.valueOf(response.getStatus()).is2xxSuccessful();
		IdempotencyResponse cached = success && wrapped != null ? IdempotencyResponse.from(wrapped) : null;

		finish(execution, cached);
	}

	private void finish(Execution execution, IdempotencyResponse response) {
		if (response == null) {
			idempotencyRedisRepository.release(execution.key(), execution.processing());
			return;
		}

		boolean completed = idempotencyRedisRepository.complete(execution.key(), execution.processing(), response);
		if (!completed) {
			log.error("멱등성 응답 저장 시 실행 소유권이 없습니다. key={}", execution.key());
		}
	}

	private String hash(String value) {
		return hash(value.getBytes(StandardCharsets.UTF_8));
	}

	private String hash(byte[] value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private record Execution(String key, IdempotencyRecord processing) {
	}
}
