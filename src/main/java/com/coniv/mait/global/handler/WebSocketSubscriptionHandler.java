package com.coniv.mait.global.handler;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.coniv.mait.domain.question.service.QuestionSetParticipantService;
import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSubscriptionHandler {
	// /topic/question/{questionSetId} 패턴을 매칭하는 정규식
	private static final Pattern QUESTION_TOPIC_PATTERN = Pattern.compile("^/topic/question/(\\d+)$");

	private final QuestionSetParticipantService questionSetParticipantService;

	@EventListener
	public void handleSubscription(SessionSubscribeEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
		String destination = headerAccessor.getDestination();
		String sessionId = headerAccessor.getSessionId();
		Principal principal = headerAccessor.getUser();

		log.info("New subscription: sessionId={}, destination={}, user={}", sessionId, destination,
			principal != null ? principal.getName() : "anonymous");

		if (destination != null && principal != null) {
			Matcher matcher = QUESTION_TOPIC_PATTERN.matcher(destination);
			if (matcher.matches()) {
				Long questionSetId = Long.parseLong(matcher.group(1));

				// 인증된 유저 정보 추출
				if (principal instanceof UserEntity user) {
					handleLiveQuestionSubscription(questionSetId, user);
				} else {
					log.warn("Principal is not UserEntity type: {}", principal.getClass().getName());
				}

				// TODO 개별 구독자에게 전송 가능하면 주석 해제
				//questionSetLiveControlService.sendCurrentQuestionStatus(questionSetId, sessionId);
			}
		}
	}

	private void handleLiveQuestionSubscription(Long questionSetId, UserEntity user) {
		try {
			questionSetParticipantService.participateLiveQuestionSet(questionSetId, user.getId());
			log.info("[소켓 연결] {} subscribed to live question set {}", user.getId(), questionSetId);
		} catch (Exception e) {
			log.error("Failed to add participant on WebSocket subscription: userId={}, questionSetId={}",
				user.getId(), questionSetId, e);
		}
	}
}
