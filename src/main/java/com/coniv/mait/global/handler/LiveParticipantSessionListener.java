package com.coniv.mait.global.handler;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import com.coniv.mait.domain.question.service.component.LiveParticipantRedisRepository;
import com.coniv.mait.global.constant.WebSocketConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveParticipantSessionListener {

	private final LiveParticipantRedisRepository liveParticipantRedisRepository;

	@EventListener
	public void onSubscribe(final SessionSubscribeEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		Long questionSetId = WebSocketConstants.parseParticipateQuestionSetId(accessor.getDestination());
		if (questionSetId == null) {
			return;
		}

		Long userId = extractUserId(accessor.getUser());
		if (userId == null) {
			log.warn("[참가자 입장 무시] 인증되지 않은 구독: destination={}", accessor.getDestination());
			return;
		}

		String sessionId = accessor.getSessionId();
		liveParticipantRedisRepository.enter(questionSetId, userId, sessionId, accessor.getSubscriptionId());
		log.info("[참가자 입장] questionSetId={} userId={} sessionId={} count={}",
			questionSetId, userId, sessionId, liveParticipantRedisRepository.getParticipantCount(questionSetId));
	}

	@EventListener
	public void onUnsubscribe(final SessionUnsubscribeEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		liveParticipantRedisRepository.leaveBySubscription(accessor.getSessionId(), accessor.getSubscriptionId());
	}

	@EventListener
	public void onDisconnect(final SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		liveParticipantRedisRepository.leaveBySession(accessor.getSessionId());
	}

	private Long extractUserId(final Principal principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken token
			&& token.getPrincipal() instanceof Long userId) {
			return userId;
		}
		return null;
	}
}
