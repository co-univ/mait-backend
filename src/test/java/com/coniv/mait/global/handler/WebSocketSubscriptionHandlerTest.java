package com.coniv.mait.global.handler;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.domain.question.service.QuestionSetParticipantService;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.domain.question.service.dto.CurrentQuestionDto;

@ExtendWith(MockitoExtension.class)
class WebSocketSubscriptionHandlerTest {

	@InjectMocks
	private WebSocketSubscriptionHandler webSocketSubscriptionHandler;

	@Mock
	private QuestionSetParticipantService questionSetParticipantService;

	@Mock
	private QuestionService questionService;

	@Mock
	private QuestionWebSocketSender questionWebSocketSender;

	@Test
	@DisplayName("문제셋 참여 토픽 구독 시 본인 참여 상태와 현재 문제 상태를 user queue로 전송한다")
	void handleSubscription_SendsMyParticipationStatus() {
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

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination("/topic/question-sets/42/participate");
		accessor.setSessionId("session-1");
		accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, null));

		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
		SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

		// when
		webSocketSubscriptionHandler.handleSubscription(event);

		// then
		then(questionWebSocketSender).should().sendMyParticipationStatus(userId, questionSetId,
			ParticipantStatus.ACTIVE, 99L, QuestionStatusType.SOLVE_PERMISSION);
	}
}
