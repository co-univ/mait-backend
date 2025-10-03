package com.coniv.mait.global.exception.handler;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import com.coniv.mait.global.exception.ExceptionCode;
import com.coniv.mait.global.response.ErrorResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

	@InjectMocks
	private GlobalExceptionHandler globalExceptionHandler;

	@Mock
	private MethodArgumentNotValidException methodArgumentNotValidException;

	@Mock
	private BindingResult bindingResult;

	@Mock
	private ServletWebRequest servletWebRequest;

	@Mock
	private HttpServletRequest httpServletRequest;

	private HttpHeaders httpHeaders;

	@BeforeEach
	void setUp() {
		httpHeaders = new HttpHeaders();
	}

	@Test
	@DisplayName("EntityNotFoundException 처리 - 엔티티를 찾을 수 없음")
	void handleEntityNotFoundException() {
		// Given
		String requestUri = "/api/test";
		String errorMessage = "Entity not found";

		// Mock 설정
		when(httpServletRequest.getRequestURI()).thenReturn(requestUri);

		// When
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleEntityNotFoundException(
			new EntityNotFoundException(errorMessage), httpServletRequest);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNotNull();

		ErrorResponse errorResponse = response.getBody();
		assertThat(errorResponse.getIsSuccess()).isFalse();
		assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(errorResponse.getMessage()).isEqualTo(ExceptionCode.ENTITY_NOT_FOUND.getMessage());
		assertNotNull(errorResponse.getReasons());
		assertThat(errorResponse.getReasons().get(0)).isEqualTo(errorMessage);

		verify(httpServletRequest).getRequestURI();
	}

	@Test
	@DisplayName("MethodArgumentNotValidException 처리 - 유효성 검증 실패")
	void handleMethodArgumentNotValid_Success() {
		// Given
		String requestUri = "/api/test";
		String errorMessage = "Validation failed";

		// FieldError 목록 생성
		List<FieldError> fieldErrors = List.of(
			new FieldError("objectName", "name", "이름은 필수입니다"),
			new FieldError("objectName", "email", "이메일 형식이 잘못되었습니다")
		);

		// Mock 설정
		when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
		when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
		when(methodArgumentNotValidException.getMessage()).thenReturn(errorMessage);
		when(servletWebRequest.getRequest()).thenReturn(httpServletRequest);
		when(httpServletRequest.getRequestURI()).thenReturn(requestUri);

		// When
		ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(
			methodArgumentNotValidException, httpHeaders, HttpStatus.BAD_REQUEST, servletWebRequest);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);

		ErrorResponse errorResponse = (ErrorResponse)response.getBody();
		assertThat(errorResponse.getIsSuccess()).isFalse();
		assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

		verify(methodArgumentNotValidException).getBindingResult();
		verify(bindingResult).getFieldErrors();
		verify(servletWebRequest).getRequest();
		verify(httpServletRequest).getRequestURI();
	}

	@Test
	@DisplayName("MethodArgumentNotValidException 처리 - 빈 에러 메시지")
	void handleMethodArgumentNotValid_EmptyErrors() {
		// Given
		String requestUri = "/api/test";
		String errorMessage = "Validation failed";
		List<FieldError> emptyFieldErrors = List.of();

		// Mock 설정
		when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
		when(bindingResult.getFieldErrors()).thenReturn(emptyFieldErrors);
		when(methodArgumentNotValidException.getMessage()).thenReturn(errorMessage);
		when(servletWebRequest.getRequest()).thenReturn(httpServletRequest);
		when(httpServletRequest.getRequestURI()).thenReturn(requestUri);

		// When
		ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(
			methodArgumentNotValidException, httpHeaders, HttpStatus.BAD_REQUEST, servletWebRequest);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);

		ErrorResponse errorResponse = (ErrorResponse)response.getBody();
		assertThat(errorResponse.getIsSuccess()).isFalse();
		assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

		verify(methodArgumentNotValidException).getBindingResult();
		verify(bindingResult).getFieldErrors();
	}

	@Test
	@DisplayName("Exception 처리 - 예상치 못한 에러")
	void handleException_UnexpectedError() {
		// Given
		Exception exception = new RuntimeException("예상치 못한 에러 발생");

		// When
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();

		ErrorResponse errorResponse = response.getBody();
		assertThat(errorResponse.getIsSuccess()).isFalse();
		assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	@DisplayName("Exception 처리 - NullPointerException")
	void handleException_NullPointerException() {
		// Given
		NullPointerException exception = new NullPointerException("NPE 발생");

		// When
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();

		ErrorResponse errorResponse = response.getBody();
		assertThat(errorResponse.getIsSuccess()).isFalse();
		assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	@DisplayName("Exception 처리 - IllegalArgumentException")
	void handleException_IllegalArgumentException() {
		// Given
		IllegalArgumentException exception = new IllegalArgumentException("잘못된 인수");

		// When
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();

		ErrorResponse errorResponse = response.getBody();
		assertThat(errorResponse.getIsSuccess()).isFalse();
		assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	@DisplayName("SQLException 처리 - 데이터베이스 오류")
	void handleSQLException() {
		// Given
		String requestUri = "/api/test";
		String sqlErrorMessage = "Database error";

		// Mock 설정
		when(httpServletRequest.getRequestURI()).thenReturn(requestUri);

		// When
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleSQLException(
			new java.sql.SQLException(sqlErrorMessage), httpServletRequest);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();

		ErrorResponse errorResponse = response.getBody();
		assertThat(errorResponse.getIsSuccess()).isFalse();
		assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(errorResponse.getMessage()).isEqualTo(ExceptionCode.DATABASE_ERROR.getMessage());
		assertNotNull(errorResponse.getReasons());
		assertThat(errorResponse.getReasons().get(0)).isEqualTo(sqlErrorMessage);

		verify(httpServletRequest).getRequestURI();
	}
}
