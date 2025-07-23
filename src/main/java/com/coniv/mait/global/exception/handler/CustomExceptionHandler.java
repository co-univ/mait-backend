package com.coniv.mait.global.exception.handler;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.coniv.mait.global.exception.ExceptionCode;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

	@ExceptionHandler(UserParameterException.class)
	public ResponseEntity<ErrorResponse> handleUserParameterException(UserParameterException exception,
		HttpServletRequest request) {
		log.info("UserParameterException 발생: {}, {}", exception.getMessage(), request.getRequestURI());
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(ExceptionCode.USER_PARAMETER_EXCEPTION, List.of(exception.getMessage())));
	}
}
