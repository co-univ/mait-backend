package com.coniv.mait.web.question.controller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.coniv.mait.domain.question.dto.QuestionStatusMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class QuestionWebSocketController {

	private final SimpMessagingTemplate messagingTemplate;

	//관리자가 특정 퀴즈셋의 상태를 변경할 때 사용
	public void broadcastQuestionStatus(Long questionSetId, QuestionStatusMessage message) {
		String destination = "/topic/question/" + questionSetId;
		messagingTemplate.convertAndSend(destination, message);

		log.info("Broadcasting question status to {}: {}", destination, message);
	}
}
