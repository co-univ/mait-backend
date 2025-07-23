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
}
