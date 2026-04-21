package com.coniv.mait.domain.team.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.dto.QuestionSetStatusMessage;
import com.coniv.mait.domain.question.enums.QuestionSetCommandType;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.global.component.EmailSender;
import com.coniv.mait.global.component.dto.EmailMessage;

@ExtendWith(MockitoExtension.class)
class TeamDeletedEventListenerTest {

	@InjectMocks
	private TeamDeletedEventListener teamDeletedEventListener;

	@Mock
	private EmailSender emailSender;

	@Mock
	private QuestionWebSocketSender questionWebSocketSender;

	@Test
	@DisplayName("팀 삭제 이벤트 처리 시 메일 실패를 수신자별로 격리하고 WebSocket 종료를 브로드캐스트한다")
	void handleTeamDeleted_EmailFailureIsolatedAndBroadcastsWebSocket() {
		// given
		TeamDeletedEvent event = TeamDeletedEvent.builder()
			.teamId(1L)
			.teamName("삭제팀")
			.recipients(List.of(
				new MemberEmailInfo("오너", "owner@example.com"),
				new MemberEmailInfo("멤버", "member@example.com")
			))
			.ongoingLiveQuestionSetIds(List.of(10L, 20L))
			.build();

		willThrow(new RuntimeException("email error"))
			.willReturn(null)
			.given(emailSender).send(any(EmailMessage.class));

		// when
		teamDeletedEventListener.handleTeamDeleted(event);

		// then
		ArgumentCaptor<EmailMessage> emailCaptor = ArgumentCaptor.forClass(EmailMessage.class);
		then(emailSender).should(times(2)).send(emailCaptor.capture());
		assertThat(emailCaptor.getAllValues())
			.extracting(message -> message.toAddresses().get(0))
			.containsExactly("owner@example.com", "member@example.com");

		then(questionWebSocketSender).should().broadcastQuestionStatus(eq(10L),
			argThat((QuestionSetStatusMessage message) -> message.getQuestionSetId().equals(10L)
				&& message.getCommandType() == QuestionSetCommandType.TEAM_DELETED));
		then(questionWebSocketSender).should().broadcastQuestionStatus(eq(20L),
			argThat((QuestionSetStatusMessage message) -> message.getQuestionSetId().equals(20L)
				&& message.getCommandType() == QuestionSetCommandType.TEAM_DELETED));
	}
}
