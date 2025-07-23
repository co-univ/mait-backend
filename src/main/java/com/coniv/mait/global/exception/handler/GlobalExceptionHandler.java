package com.coniv.mait.global.exception.handler;

import java.util.List;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.coniv.mait.global.exception.ExceptionCode;
import com.coniv.mait.global.response.ErrorResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException exception,
		HttpServletRequest request) {
		log.info("EntityNotFoundException 발생: {}, 경로: {}", exception.getMessage(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ErrorResponse.of(ExceptionCode.ENTITY_NOT_FOUND, List.of(exception.getMessage())));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
		HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ServletWebRequest servletWebRequest = (ServletWebRequest)request;
		HttpServletRequest httpServletRequest = servletWebRequest.getRequest();

		List<String> messages = exception.getBindingResult().getFieldErrors().stream()
			.map(DefaultMessageSourceResolvable::getDefaultMessage)
			.toList();

		log.info("MethodArgumentNotValidException 발생: requestURI={}, error={}", httpServletRequest.getRequestURI(),
			exception.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ErrorResponse.of(ExceptionCode.USER_INPUT_EXCEPTION, messages));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {
		log.error("[unexpected error]", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.from(ExceptionCode.UNEXPECTED_EXCEPTION));
	}
}
