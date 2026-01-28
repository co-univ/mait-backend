package com.coniv.mait.global.auth.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.coniv.mait.global.exception.CommonExceptionCode;
import com.coniv.mait.global.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {

		log.info("Unauthorized error: {}", authException.getMessage());
		CommonExceptionCode exceptionCode = CommonExceptionCode.JWT_AUTH_EXCEPTION;
		Throwable cause = authException.getCause();
		if (cause instanceof ExpiredJwtException) {
			exceptionCode = CommonExceptionCode.JWT_EXPIRED;
		}
		response.setStatus(exceptionCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ErrorResponse errorResponse = ErrorResponse.of(exceptionCode,
			List.of(authException.getMessage()));

		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}
}
