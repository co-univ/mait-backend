package com.coniv.mait.web.question.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.domain.question.service.QuestionSetParticipantService;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.domain.question.service.dto.CurrentQuestionDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class QuestionWebSocketController {

	private final QuestionService questionService;
	private final QuestionSetParticipantService questionSetParticipantService;
	private final QuestionWebSocketSender questionWebSocketSender;

	@MessageMapping("/question-sets/{questionSetId}/participation-status")
	public void requestParticipationStatus(
		@DestinationVariable Long questionSetId,
		Principal principal) {

		Long userId = extractUserId(principal);
		if (userId == null) {
			log.warn("인증되지 않은 사용자의 participation-status 요청: principal={}", principal);
			return;
		}

		try {
			ParticipantDto participant = questionSetParticipantService.participateLiveQuestionSet(
				questionSetId, userId);
			CurrentQuestionDto currentQuestion = questionService.findCurrentQuestion(questionSetId);
			questionWebSocketSender.sendMyParticipationStatus(userId, questionSetId,
				participant.getStatus(), currentQuestion.getQuestionId(),
				currentQuestion.getQuestionStatus());
			log.info(
				"[초기 상태 전송] userId={} questionSetId={} status={} questionId={} questionStatus={}",
				userId, questionSetId, participant.getStatus(),
				currentQuestion.getQuestionId(), currentQuestion.getQuestionStatus());
		} catch (Exception e) {
			log.error("Failed to process participation status request: userId={}, questionSetId={}",
				userId, questionSetId, e);
		}
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
