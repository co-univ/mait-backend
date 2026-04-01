package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.coniv.mait.domain.question.service.QuestionSetLiveControlService;
import com.coniv.mait.global.response.WebSocketErrorResponse;

@ExtendWith(MockitoExtension.class)
class QuestionWebSocketControllerTest {

	@InjectMocks
	private QuestionWebSocketController questionWebSocketController;

	@Mock
	private QuestionSetLiveControlService questionSetLiveControlService;

	@Test
	@DisplayName("participation-status 요청 시 서비스에 위임한다")
	void requestParticipationStatus_DelegatesToService() {
		// given
		Long questionSetId = 42L;
		Long userId = 10L;
		UsernamePasswordAuthenticationToken principal =
			new UsernamePasswordAuthenticationToken(userId, null, null);

		// when
		questionWebSocketController.requestParticipationStatus(questionSetId, principal);

		// then
		then(questionSetLiveControlService).should().handleParticipation(questionSetId, userId);
	}

	@Test
	@DisplayName("인증되지 않은 사용자의 요청은 무시한다")
	void requestParticipationStatus_IgnoresUnauthenticated() {
		// when
		questionWebSocketController.requestParticipationStatus(42L, null);

		// then
		then(questionSetLiveControlService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("예외 발생 시 에러 응답을 반환한다")
	void handleException_ReturnsErrorResponse() {
		// given
		Exception exception = new RuntimeException("문제셋을 찾을 수 없습니다");

		// when
		WebSocketErrorResponse response = questionWebSocketController.handleException(exception);

		// then
		assertThat(response.isSuccess()).isFalse();
		assertThat(response.getMessage()).isEqualTo("문제셋을 찾을 수 없습니다");
	}
}
