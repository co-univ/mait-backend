package com.coniv.mait.global.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.coniv.mait.domain.question.service.QuestionSetLiveControlService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSubscriptionHandler {

	private final QuestionSetLiveControlService questionSetLiveControlService;

	// /topic/question/{questionSetId} 패턴을 매칭하는 정규식
	private static final Pattern QUESTION_TOPIC_PATTERN = Pattern.compile("^/topic/question/(\\d+)$");

	@EventListener
	public void handleSubscription(SessionSubscribeEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
		String destination = headerAccessor.getDestination();
		String sessionId = headerAccessor.getSessionId();

		log.info("New subscription: sessionId={}, destination={}", sessionId, destination);

		if (destination != null) {
			Matcher matcher = QUESTION_TOPIC_PATTERN.matcher(destination);
			if (matcher.matches()) {
				Long questionSetId = Long.parseLong(matcher.group(1));
				// TODO 개별 구독자에게 전송 가능하면 주석 해제
				//questionSetLiveControlService.sendCurrentQuestionStatus(questionSetId, sessionId);
			}
		}
	}
}
