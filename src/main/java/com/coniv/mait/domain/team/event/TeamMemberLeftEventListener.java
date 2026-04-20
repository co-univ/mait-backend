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
		sendEmail(event.memberEmail(),
			TeamMemberLeftEmailTemplate.memberSubject(event),
			TeamMemberLeftEmailTemplate.memberTextBody(event),
			TeamMemberLeftEmailTemplate.memberHtmlBody(event),
			event.teamName());
		sendEmail(event.ownerEmail(),
			TeamMemberLeftEmailTemplate.ownerSubject(event),
			TeamMemberLeftEmailTemplate.ownerTextBody(event),
			TeamMemberLeftEmailTemplate.ownerHtmlBody(event),
			event.teamName());
	}

	private void sendEmail(final String recipient, final String subject, final String textBody, final String htmlBody,
		final String teamName) {
		try {
			emailSender.send(EmailMessage.builder()
				.toAddresses(List.of(recipient))
				.subject(subject)
				.textBody(textBody)
				.htmlBody(htmlBody)
				.build());
		} catch (Exception exception) {
			log.warn("[팀 탈퇴 메일 발송 실패] recipient={}, teamName={}", recipient, teamName, exception);
		}
	}
}
