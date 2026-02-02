package com.coniv.mait.global.interceptor;

import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Order(value = Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@Component
public class WebSocketLoggingInterceptor implements ExecutorChannelInterceptor {

	private static final String REQUEST_ID = "requestId";

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		String requestId = null;

		Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
		if (sessionAttributes != null && sessionAttributes.containsKey(REQUEST_ID)) {
			requestId = (String) sessionAttributes.get(REQUEST_ID);
		}

		if (requestId == null || requestId.isEmpty()) {
			requestId = UUID.randomUUID().toString();
		}

		StompHeaderAccessor newAccessor = StompHeaderAccessor.wrap(message);
		newAccessor.setNativeHeader(REQUEST_ID, requestId);
		MDC.put(REQUEST_ID, requestId);

		return MessageBuilder.createMessage(message.getPayload(), newAccessor.getMessageHeaders());
	}

	@Override
	public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
		MDC.remove(REQUEST_ID);
	}

	@Override
	public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		String requestId = accessor.getFirstNativeHeader(REQUEST_ID);

		if (requestId != null) {
			MDC.put(REQUEST_ID, requestId);
		}
		return message;
	}

	@Override
	public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
		MDC.remove(REQUEST_ID);
	}
}
