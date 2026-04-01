package com.coniv.mait.global.handler;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketSubscriptionHandler {

	@EventListener
	public void handleSubscription(SessionSubscribeEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
		String destination = headerAccessor.getDestination();
		String sessionId = headerAccessor.getSessionId();
		Principal principal = headerAccessor.getUser();

		log.info("New subscription: sessionId={}, destination={}, userId={}", sessionId, destination,
			principal != null ? principal.getName() : "anonymous");
	}
}
