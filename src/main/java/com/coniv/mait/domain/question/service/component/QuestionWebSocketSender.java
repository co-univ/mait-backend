package com.coniv.mait.domain.question.service.component;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.dto.QuestionSetStatusMessage;
import com.coniv.mait.domain.question.dto.QuestionStatusMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionWebSocketSender {

	private final SimpMessagingTemplate messagingTemplate;

	//관리자가 특정 퀴즈셋의 상태를 변경할 때 사용
	public void broadcastQuestionStatus(Long questionSetId, QuestionStatusMessage message) {
		String destination = "/topic/question/" + questionSetId;
		messagingTemplate.convertAndSend(destination, message);

		log.info("Broadcasting question status to {}: {}", destination, message);
	}

	public void broadcastQuestionStatus(Long questionSetId, QuestionSetStatusMessage message) {
		String destination = "/topic/question/" + questionSetId;
		messagingTemplate.convertAndSend(destination, message);

		log.info("Broadcasting question set status to {}: {}", destination, message);
	}
}

