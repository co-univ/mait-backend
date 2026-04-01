package com.coniv.mait.web.question.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import com.coniv.mait.domain.question.service.QuestionSetLiveControlService;
import com.coniv.mait.global.response.WebSocketErrorResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class QuestionWebSocketController {

	private final QuestionSetLiveControlService questionSetLiveControlService;

	@MessageMapping("/question-sets/{questionSetId}/participation-status")
	public void requestParticipationStatus(
		@DestinationVariable Long questionSetId,
		Principal principal) {

		Long userId = extractUserId(principal);
		if (userId == null) {
			log.warn("인증되지 않은 사용자의 participation-status 요청: principal={}", principal);
			return;
		}

		questionSetLiveControlService.handleParticipation(questionSetId, userId);
	}

	@MessageExceptionHandler
	@SendToUser("/queue/errors")
	public WebSocketErrorResponse handleException(Exception exception) {
		log.error("WebSocket 메시지 처리 중 오류 발생", exception);
		return WebSocketErrorResponse.from(exception.getMessage());
	}

	private Long extractUserId(final Principal principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken authToken) {
			Object userPrincipal = authToken.getPrincipal();
			if (userPrincipal instanceof Long userId) {
				return userId;
			}
		}
		return null;
	}
}
