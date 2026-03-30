package com.coniv.mait.domain.question.event;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.dto.ParticipantDto;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;

@ExtendWith(MockitoExtension.class)
class NewParticipantEventListenerTest {

	@InjectMocks
	private NewParticipantEventListener newParticipantEventListener;

	@Mock
	private QuestionWebSocketSender questionWebSocketSender;

	@Test
	@DisplayName("이벤트 수신 시 MAKER 토픽으로 새 참여자를 브로드캐스트한다")
	void handleNewParticipantEvent_Success_BroadcastsToMaker() {
		// given
		Long questionSetId = 1L;
		ParticipantDto participant = ParticipantDto.builder()
			.userId(10L)
			.participantName("testUser")
			.build();

		NewParticipantEvent event = NewParticipantEvent.builder()
			.questionSetId(questionSetId)
			.participant(participant)
			.build();

		// when
		newParticipantEventListener.handleNewParticipantEvent(event);

		// then
		then(questionWebSocketSender).should().broadcastNewParticipantToMaker(questionSetId, participant);
	}

	@Test
	@DisplayName("브로드캐스트 중 예외 발생 시 예외를 전파하지 않는다")
	void handleNewParticipantEvent_ExceptionThrown_DoesNotPropagate() {
		// given
		Long questionSetId = 1L;
		ParticipantDto participant = ParticipantDto.builder()
			.userId(10L)
			.participantName("testUser")
			.build();

		NewParticipantEvent event = NewParticipantEvent.builder()
			.questionSetId(questionSetId)
			.participant(participant)
			.build();

		willThrow(new RuntimeException("WebSocket error"))
			.given(questionWebSocketSender).broadcastNewParticipantToMaker(questionSetId, participant);

		// when & then (예외가 전파되지 않음)
		newParticipantEventListener.handleNewParticipantEvent(event);
	}
}
