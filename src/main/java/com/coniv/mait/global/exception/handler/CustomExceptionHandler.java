package com.coniv.mait.global.exception.handler;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.coniv.mait.global.exception.ExceptionCode;
import com.coniv.mait.global.exception.custom.LoginFailException;
import com.coniv.mait.global.exception.custom.QuestionSetLiveException;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;
import com.coniv.mait.global.exception.custom.TeamInviteFailException;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

	@ExceptionHandler(LoginFailException.class)
	public ResponseEntity<ErrorResponse> handleLoginFailException(LoginFailException exception,
		HttpServletRequest request) {
		log.info("LoginFailException 발생: {}, {}", exception.getMessage(), request.getRequestURI());
		return ResponseEntity.badRequest().body(ErrorResponse.from(ExceptionCode.LOGIN_FAIL_EXCEPTION));
	}

	@ExceptionHandler(UserParameterException.class)
	public ResponseEntity<ErrorResponse> handleUserParameterException(UserParameterException exception,
		HttpServletRequest request) {
		log.info("UserParameterException 발생: {}, {}", exception.getMessage(), request.getRequestURI());
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(ExceptionCode.USER_PARAMETER_EXCEPTION, List.of(exception.getMessage())));
	}

	@ExceptionHandler(ResourceNotBelongException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotBelongException(ResourceNotBelongException exception,
		HttpServletRequest request) {
		log.info("ResourceNotBelongException 발생: {}, {}", exception.getMessage(), request.getRequestURI());
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(ExceptionCode.RESOURCE_NOT_BELONG_EXCEPTION, List.of(exception.getMessage())));
	}

	@ExceptionHandler(QuestionSetLiveException.class)
	public ResponseEntity<ErrorResponse> handleQuestionSetLiveException(QuestionSetLiveException exception,
		HttpServletRequest request) {
		log.info("QuestionSetLiveException 발생: {}, {}", exception.getMessage(), request.getRequestURI());
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(ExceptionCode.Question_SET_LIVE_EXCEPTION, List.of(exception.getMessage())));
	}

	@ExceptionHandler(TeamInviteFailException.class)
	public ResponseEntity<ErrorResponse> handleTeamInviteFailException(TeamInviteFailException exception,
		HttpServletRequest request) {
		log.info("TeamInviteFailException 발생: {}, {}", exception.getMessage(), request.getRequestURI());
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(ExceptionCode.TEAM_INVITE_FAIL_EXCEPTION, List.of(exception.getMessage())));
	}
}
