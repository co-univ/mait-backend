package com.coniv.mait.global.interceptor.idempotency;

import java.util.Objects;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.coniv.mait.global.exception.CommonExceptionCode;
import com.coniv.mait.global.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

	private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

	private final IdempotencyRedisRepository idempotencyRedisRepository;

	private final ObjectMapper objectMapper;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws
		Exception {

		if (!HttpMethod.POST.name().equals(request.getMethod())) {
			return true;
		}

		final String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);
		if (idempotencyKey == null || idempotencyKey.isEmpty()) {
			return true;
		}

		IdempotencyStatus status = idempotencyRedisRepository.getStatus(idempotencyKey);

		if (status == null) {
			idempotencyRedisRepository.setProcessing(idempotencyKey);
			return true;
		}

		if (status == IdempotencyStatus.PROCESSING) {
			response.setStatus(HttpStatus.CONFLICT.value());
			response.setContentType("application/json; charset=UTF-8");
			response.getWriter()
				.write(objectMapper.writeValueAsString(ErrorResponse.from(CommonExceptionCode.PROCESSING)));
			log.info("[요청은 왔지만 아직 처리 중]");
			return false;
		}

		Object cachedResponse = idempotencyRedisRepository.getResponse(idempotencyKey);
		if (status == IdempotencyStatus.COMPLETED && !Objects.isNull(cachedResponse)) {
			response.setContentType("application/json");
			response.getWriter().write(objectMapper.writeValueAsString(cachedResponse));
			return false;
		}

		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
		ModelAndView modelAndView) throws Exception {
		String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);
		if (idempotencyKey == null) {
			return;
		}

		final ContentCachingResponseWrapper responseWrapper = (ContentCachingResponseWrapper)response;

		if (responseWrapper.getStatus() != HttpStatus.OK.value()) {
			log.info("[멱등성 처리 실패] 응답 상태: {}", responseWrapper.getStatus());
			return;
		}

		idempotencyRedisRepository.setCompleted(idempotencyKey,
			objectMapper.readTree(responseWrapper.getContentAsByteArray()));

		responseWrapper.copyBodyToResponse();
	}
}
