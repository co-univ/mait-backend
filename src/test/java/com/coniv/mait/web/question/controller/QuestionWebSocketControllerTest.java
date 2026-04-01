package com.coniv.mait.web.question.controller;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.domain.question.service.QuestionSetParticipantService;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.domain.question.service.dto.CurrentQuestionDto;

@ExtendWith(MockitoExtension.class)
class QuestionWebSocketControllerTest {

	@InjectMocks
	private QuestionWebSocketController questionWebSocketController;

	@Mock
	private QuestionSetParticipantService questionSetParticipantService;

	@Mock
	private QuestionService questionService;

	@Mock
	private QuestionWebSocketSender questionWebSocketSender;

	@Test
	@DisplayName("participation-status 요청 시 참여 처리 후 본인 상태를 user queue로 전송한다")
	void requestParticipationStatus_SendsMyParticipationStatus() {
		// given
		Long questionSetId = 42L;
		Long userId = 10L;
		ParticipantDto participant = ParticipantDto.builder()
			.userId(userId)
			.status(ParticipantStatus.ACTIVE)
			.build();
		CurrentQuestionDto currentQuestion = CurrentQuestionDto.of(questionSetId, 99L,
			QuestionStatusType.SOLVE_PERMISSION);
		given(questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId))
			.willReturn(participant);
		given(questionService.findCurrentQuestion(questionSetId)).willReturn(currentQuestion);

		UsernamePasswordAuthenticationToken principal =
			new UsernamePasswordAuthenticationToken(userId, null, null);

		// when
		questionWebSocketController.requestParticipationStatus(questionSetId, principal);

		// then
		then(questionWebSocketSender).should().sendMyParticipationStatus(userId, questionSetId,
			ParticipantStatus.ACTIVE, 99L, QuestionStatusType.SOLVE_PERMISSION);
	}

	@Test
	@DisplayName("인증되지 않은 사용자의 요청은 무시한다")
	void requestParticipationStatus_IgnoresUnauthenticated() {
		// when
		questionWebSocketController.requestParticipationStatus(42L, null);

		// then
		then(questionSetParticipantService).shouldHaveNoInteractions();
		then(questionWebSocketSender).shouldHaveNoInteractions();
	}
}
