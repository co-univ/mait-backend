package com.coniv.mait.domain.question.service.component;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LiveParticipantBroadcasterTest {

	@InjectMocks
	private LiveParticipantBroadcaster liveParticipantBroadcaster;

	@Mock
	private LiveParticipantRedisRepository liveParticipantRedisRepository;

	@Mock
	private QuestionWebSocketSender questionWebSocketSender;

	@Test
	@DisplayName("현재 참가자 수를 조회해 participate 토픽으로 전파한다")
	void broadcastCount_sendsCurrentCount() {
		given(liveParticipantRedisRepository.getParticipantCount(7L)).willReturn(5L);

		liveParticipantBroadcaster.broadcastCount(7L);

		then(questionWebSocketSender).should().broadcastParticipantCount(7L, 5L);
	}
}
