package com.coniv.mait.domain.question.service.component;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.dto.QuestionSetParticipationStatusMessage;
import com.coniv.mait.domain.question.enums.ParticipantStatus;

@ExtendWith(MockitoExtension.class)
class QuestionWebSocketSenderTest {

	@InjectMocks
	private QuestionWebSocketSender questionWebSocketSender;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Test
	@DisplayName("MAKER 토픽으로 새 참여자 정보를 전송한다")
	void broadcastNewParticipantToMaker_SendsToManageTopic() {
		// given
		Long questionSetId = 42L;
		ParticipantDto participant = ParticipantDto.builder()
			.userId(10L)
			.participantName("testUser")
			.build();

		// when
		questionWebSocketSender.broadcastNewParticipantToMaker(questionSetId, participant);

		// then
		then(messagingTemplate).should().convertAndSend(
			"/topic/question-sets/42/manage", participant);
	}

	@Test
	@DisplayName("본인 참여 상태를 user queue로 전송한다")
	void sendMyParticipationStatus_SendsToUserQueue() {
		// given
		Long userId = 10L;
		Long questionSetId = 42L;

		// when
		questionWebSocketSender.sendMyParticipationStatus(userId, questionSetId, ParticipantStatus.ELIMINATED);

		// then
		then(messagingTemplate).should().convertAndSendToUser(
			eq("10"),
			eq("/queue/question-sets/42/participation-status"),
			argThat(message -> {
				QuestionSetParticipationStatusMessage statusMessage =
					(QuestionSetParticipationStatusMessage)message;
				return statusMessage.getQuestionSetId().equals(questionSetId)
					&& statusMessage.getParticipantStatus() == ParticipantStatus.ELIMINATED;
			}));
	}
}
