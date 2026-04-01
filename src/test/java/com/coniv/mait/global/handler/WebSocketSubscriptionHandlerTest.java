package com.coniv.mait.global.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@ExtendWith(MockitoExtension.class)
class WebSocketSubscriptionHandlerTest {

	@InjectMocks
	private WebSocketSubscriptionHandler webSocketSubscriptionHandler;

	@Test
	@DisplayName("구독 이벤트 수신 시 로깅만 수행한다")
	void handleSubscription_LogsOnly() {
		// given
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination("/topic/question-sets/42/participate");
		accessor.setSessionId("session-1");
		accessor.setUser(new UsernamePasswordAuthenticationToken(10L, null, null));

		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
		SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

		// when & then (로깅만 수행, 예외 없이 정상 처리)
		webSocketSubscriptionHandler.handleSubscription(event);
	}
}
