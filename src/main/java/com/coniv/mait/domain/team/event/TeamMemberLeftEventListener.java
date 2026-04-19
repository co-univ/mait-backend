package com.coniv.mait.domain.team.event;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.coniv.mait.global.component.EmailSender;
import com.coniv.mait.global.component.dto.EmailMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamMemberLeftEventListener {

	private final EmailSender emailSender;

	@Async("maitThreadPoolExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleTeamMemberLeft(final TeamMemberLeftEvent event) {
		if (event.recipientEmails().isEmpty()) {
			log.info("[팀 탈퇴 메일] 수신할 관리자 없음. teamName={}", event.teamName());
			return;
		}

		String subject = TeamMemberLeftEmailTemplate.subject(event);
		String textBody = TeamMemberLeftEmailTemplate.textBody(event);
		String htmlBody = TeamMemberLeftEmailTemplate.htmlBody(event);

		for (String recipientEmail : event.recipientEmails()) {
			try {
				emailSender.send(EmailMessage.builder()
					.toAddresses(List.of(recipientEmail))
					.subject(subject)
					.textBody(textBody)
					.htmlBody(htmlBody)
					.build());
			} catch (Exception exception) {
				log.error("[팀 탈퇴 메일 발송 실패] recipientEmail={}, teamName={}",
					recipientEmail, event.teamName(), exception);
			}
		}
	}
}
