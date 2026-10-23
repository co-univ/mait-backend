package com.coniv.mait.global.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyRequestWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ContentWrappingFilter extends OncePerRequestFilter {

	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		final ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
		String key = request.getHeader("Idempotency-Key");
		HttpServletRequest wrappedRequest = request;
		String path = request.getRequestURI().substring(request.getContextPath().length());
		if ("POST".equals(request.getMethod()) && key != null && !key.isBlank()
			&& PATH_MATCHER.match(IdempotencyInterceptor.PATH_PATTERN, path)) {
			wrappedRequest = new IdempotencyRequestWrapper(request);
		}
		filterChain.doFilter(wrappedRequest, responseWrapper);
		responseWrapper.copyBodyToResponse();
	}
}
