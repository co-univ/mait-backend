package com.coniv.mait.global.handler;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.coniv.mait.domain.question.service.QuestionSetParticipantService;
import com.coniv.mait.global.constant.WebSocketConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSubscriptionHandler {
	private static final Pattern QUESTION_TOPIC_PATTERN = Pattern.compile(WebSocketConstants.QUESTION_SET_PARTICIPATE_TOPIC_PATTERN);

	private final QuestionSetParticipantService questionSetParticipantService;

	@EventListener
	public void handleSubscription(SessionSubscribeEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
		String destination = headerAccessor.getDestination();
		String sessionId = headerAccessor.getSessionId();
		Principal principal = headerAccessor.getUser();

		log.info("New subscription: sessionId={}, destination={}, userId={}", sessionId, destination,
			principal != null ? principal.getName() : "anonymous");

		if (destination != null && principal != null) {
			Matcher matcher = QUESTION_TOPIC_PATTERN.matcher(destination);
			if (matcher.matches()) {
				Long questionSetId = Long.parseLong(matcher.group(1));
				Long userId = extractUserIdFromPrincipal(principal);
				if (userId != null) {
					handleLiveQuestionSubscription(questionSetId, userId);
				} else {
					log.warn("인증되지 않은 사용자의 구독 시도: principal type={}", principal.getClass().getName());
				}
			}
		}
	}

	private void handleLiveQuestionSubscription(Long questionSetId, Long userId) {
		try {
			questionSetParticipantService.participateLiveQuestionSet(questionSetId, userId);
			log.info("[소켓 연결] userId={} subscribed to live question set {}", userId, questionSetId);
		} catch (Exception e) {
			log.error("Failed to add participant on WebSocket subscription: userId={}, questionSetId={}",
				userId, questionSetId, e);
		}
	}

	private Long extractUserIdFromPrincipal(final Principal principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken authToken) {
			Object userPrincipal = authToken.getPrincipal();
			if (userPrincipal instanceof Long userId) {
				return userId;
			}
		}
		return null;
	}
}
