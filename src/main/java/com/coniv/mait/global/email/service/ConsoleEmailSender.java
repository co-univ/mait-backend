package com.coniv.mait.global.email.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.coniv.mait.global.component.EmailSender;
import com.coniv.mait.global.component.dto.EmailMessage;
import com.coniv.mait.global.component.dto.EmailSendResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "mait.email", name = "provider", havingValue = "console")
public class ConsoleEmailSender implements EmailSender {

	private static final String MESSAGE_ID = "console";

	@Override
	public EmailSendResult send(EmailMessage message) {
		log.info("[email skipped] toCount: {}, ccCount: {}, bccCount: {}, subject: {}",
			message.toAddresses().size(), message.ccAddresses().size(), message.bccAddresses().size(),
			message.subject());
		return EmailSendResult.of(MESSAGE_ID);
	}
}
