package com.coniv.mait.global.exception.handler;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

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

import com.coniv.mait.domain.question.exception.QuestionExceptionCode;
import com.coniv.mait.domain.question.exception.QuestionStatusException;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.global.exception.code.S3ExceptionCode;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;
import com.coniv.mait.global.exception.custom.S3FileException;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class CustomExceptionHandlerTest {

	@InjectMocks
	private CustomExceptionHandler customExceptionHandler;

	@Mock
	private HttpServletRequest httpServletRequest;

	private HttpHeaders httpHeaders;

	@BeforeEach
	void setUp() {
		httpHeaders = new HttpHeaders();
	}

	@Test
	@DisplayName("UserParameterException 처리 테스트")
	void handleUserParameterException() {
		// given
		String errorMessage = "Invalid user parameter";
		UserParameterException exception = new UserParameterException(errorMessage);

		// when
		ResponseEntity<ErrorResponse> errorResponse = customExceptionHandler.handleUserParameterException(
			exception, httpServletRequest);

		// then
		assertNotNull(errorResponse);
		assertTrue(errorResponse.getBody().getReasons().contains(errorMessage));
		assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("ResourceNotBelongException 처리 테스트")
	void handleResourceNotBelongException() {
		// given
		String errorMessage = "Resource does not belong to the user";
		ResourceNotBelongException exception = new ResourceNotBelongException(errorMessage);

		// when
		ResponseEntity<ErrorResponse> errorResponse = customExceptionHandler.handleResourceNotBelongException(
			exception, httpServletRequest);

		// then
		assertNotNull(errorResponse);
		assertTrue(errorResponse.getBody().getReasons().contains(errorMessage));
		assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("S3FileException 처리 테스트 - PUT 오류")
	void handleS3FileExceptionForPut() {
		// given
		String bucket = "mait-image-bucket";
		String key = "images/test.jpg";
		S3ExceptionCode code = S3ExceptionCode.PUT;
		S3FileException exception = new S3FileException(code, bucket, key);

		// when
		ResponseEntity<ErrorResponse> errorResponse = customExceptionHandler.handleS3FileException(
			exception, httpServletRequest);

		// then
		assertNotNull(errorResponse);
		assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertTrue(errorResponse.getBody().getReasons().contains(code.getMessage()));
		assertThat(errorResponse.getBody().getCode()).isEqualTo("F-001");
	}

	@Test
	@DisplayName("S3FileException 처리 테스트 - INVALID_TYPE 오류")
	void handleS3FileExceptionForInvalidType() {
		// given
		String bucket = "mait-image-bucket";
		String key = "images/test.txt";
		S3ExceptionCode code = S3ExceptionCode.INVALID_TYPE;
		S3FileException exception = new S3FileException(code, bucket, key);

		// when
		ResponseEntity<ErrorResponse> errorResponse = customExceptionHandler.handleS3FileException(
			exception, httpServletRequest);

		// then
		assertNotNull(errorResponse);
		assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertTrue(errorResponse.getBody().getReasons().contains(code.getMessage()));
		assertThat(errorResponse.getBody().getMessage()).isEqualTo("S3 파일 처리 중 오류가 발생했습니다.");
	}

	@Test
	@DisplayName("UserRoleException 처리 테스트")
	void handleUserRoleException() {
		// given
		String errorMessage = "문제 세트 생성 권한이 없습니다.";
		UserRoleException exception = new UserRoleException(errorMessage);

		// when
		ResponseEntity<ErrorResponse> errorResponse = customExceptionHandler.handleUserRoleException(
			exception, httpServletRequest);

		// then
		assertNotNull(errorResponse);
		assertTrue(errorResponse.getBody().getReasons().contains(errorMessage));
		assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(errorResponse.getBody().getCode()).isEqualTo("C-008");
		assertThat(errorResponse.getBody().getMessage()).isEqualTo("사용자 권한이 부족합니다.");
	}

	@Test
	@DisplayName("QuestionSolveException 처리 테스트")
	void handleQuestionSolveException() {
		// given
		QuestionSolvingException exception = new QuestionSolvingException(QuestionSolveExceptionCode.CANNOT_SOLVE);

		// when
		ResponseEntity<ErrorResponse> errorResponse = customExceptionHandler.handleQuestionSolveException(
			exception, httpServletRequest);

		// then
		assertNotNull(errorResponse);
		assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(errorResponse.getBody().getCode()).isEqualTo("QS-001");
	}

	@Test
	@DisplayName("QuestionStatusException 처리 테스트 - 처리 불가능한 문제 타입")
	void handleQuestionStatusException() {
		// given
		QuestionExceptionCode exceptionCode = QuestionExceptionCode.UNAVAILABLE_TYPE;
		QuestionStatusException exception = new QuestionStatusException(exceptionCode);

		// when
		ResponseEntity<ErrorResponse> errorResponse = customExceptionHandler.handleQuestionStatusException(
			exception, httpServletRequest);

		// then
		assertNotNull(errorResponse);
		assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(errorResponse.getBody().getCode()).isEqualTo("T-001");
		assertThat(errorResponse.getBody().getMessage()).isEqualTo("해당 타입의 문제는 처리가 불가능합니다.");
	}
}
