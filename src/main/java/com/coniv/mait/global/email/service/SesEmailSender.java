package com.coniv.mait.global.email.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.coniv.mait.global.component.EmailSender;
import com.coniv.mait.global.component.dto.EmailMessage;
import com.coniv.mait.global.component.dto.EmailSendResult;
import com.coniv.mait.global.config.property.EmailProperty;
import com.coniv.mait.global.exception.code.EmailExceptionCode;
import com.coniv.mait.global.exception.custom.EmailSendException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mait.email", name = "provider", havingValue = "ses", matchIfMissing = true)
public class SesEmailSender implements EmailSender {

	private static final String CHARSET = "UTF-8";

	private final EmailProperty emailProperty;

	private final SesV2Client sesV2Client;

	@Override
	public EmailSendResult send(EmailMessage message) {
		SendEmailRequest.Builder builder = SendEmailRequest.builder()
			.fromEmailAddress(emailProperty.getFromAddress())
			.destination(toDestination(message))
			.content(toEmailContent(message));

		List<String> replyToAddresses = resolveReplyToAddresses(message);
		if (!replyToAddresses.isEmpty()) {
			builder.replyToAddresses(replyToAddresses);
		}
		if (hasText(emailProperty.getConfigurationSetName())) {
			builder.configurationSetName(emailProperty.getConfigurationSetName().trim());
		}

		try {
			SendEmailRequest request = builder.build();
			SendEmailResponse response = sesV2Client.sendEmail(request);
			return EmailSendResult.of(response.messageId());
		} catch (SesV2Exception | SdkClientException exception) {
			log.error("[SES 이메일 발송 실패] toCount: {}, subject: {}",
				message.toAddresses().size(), message.subject(), exception);
			throw new EmailSendException(EmailExceptionCode.SEND, "ses", exception);
		}
	}

	private Destination toDestination(EmailMessage message) {
		Destination.Builder builder = Destination.builder()
			.toAddresses(message.toAddresses());

		if (!message.ccAddresses().isEmpty()) {
			builder.ccAddresses(message.ccAddresses());
		}
		if (!message.bccAddresses().isEmpty()) {
			builder.bccAddresses(message.bccAddresses());
		}

		return builder.build();
	}

	private EmailContent toEmailContent(EmailMessage message) {
		return EmailContent.builder()
			.simple(Message.builder()
				.subject(toContent(message.subject()))
				.body(toBody(message))
				.build())
			.build();
	}

	private Body toBody(EmailMessage message) {
		Body.Builder builder = Body.builder();

		if (message.hasTextBody()) {
			builder.text(toContent(message.textBody()));
		}
		if (message.hasHtmlBody()) {
			builder.html(toContent(message.htmlBody()));
		}

		return builder.build();
	}

	private Content toContent(String data) {
		return Content.builder()
			.data(data)
			.charset(CHARSET)
			.build();
	}

	private List<String> resolveReplyToAddresses(EmailMessage message) {
		if (!message.replyToAddresses().isEmpty()) {
			return message.replyToAddresses();
		}
		if (!hasText(emailProperty.getReplyToAddress())) {
			return List.of();
		}
		return List.of(emailProperty.getReplyToAddress().trim());
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
