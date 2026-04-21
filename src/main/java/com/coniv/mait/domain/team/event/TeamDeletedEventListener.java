package com.coniv.mait.domain.team.event;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.coniv.mait.domain.question.dto.QuestionSetStatusMessage;
import com.coniv.mait.domain.question.enums.QuestionSetCommandType;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.global.component.EmailSender;
import com.coniv.mait.global.component.dto.EmailMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamDeletedEventListener {

	private final EmailSender emailSender;
	private final QuestionWebSocketSender questionWebSocketSender;

	@Async("maitThreadPoolExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleTeamDeleted(final TeamDeletedEvent event) {
		event.recipients().forEach(recipient -> sendEmail(event, recipient));
		event.ongoingLiveQuestionSetIds().forEach(this::broadcastTeamDeleted);
	}

	private void sendEmail(final TeamDeletedEvent event, final MemberEmailInfo recipient) {
		try {
			emailSender.send(EmailMessage.builder()
				.toAddresses(List.of(recipient.email()))
				.subject(TeamDeletedEmailTemplate.subject(event))
				.textBody(TeamDeletedEmailTemplate.textBody(event, recipient))
				.htmlBody(TeamDeletedEmailTemplate.htmlBody(event, recipient))
				.build());
		} catch (Exception exception) {
			log.warn("[팀 삭제 메일 발송 실패] recipient={}, teamId={}, teamName={}",
				recipient.email(), event.teamId(), event.teamName(), exception);
		}
	}

	private void broadcastTeamDeleted(final Long questionSetId) {
		QuestionSetStatusMessage message = QuestionSetStatusMessage.builder()
			.questionSetId(questionSetId)
			.commandType(QuestionSetCommandType.TEAM_DELETED)
			.build();
		questionWebSocketSender.broadcastQuestionStatus(questionSetId, message);
	}
}
