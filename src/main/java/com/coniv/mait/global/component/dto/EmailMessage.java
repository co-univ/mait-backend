package com.coniv.mait.global.component.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record EmailMessage(
	List<String> toAddresses,
	List<String> ccAddresses,
	List<String> bccAddresses,
	String subject,
	String textBody,
	String htmlBody,
	List<String> replyToAddresses
) {

	public EmailMessage {
		toAddresses = copyAddresses(toAddresses);
		ccAddresses = copyAddresses(ccAddresses);
		bccAddresses = copyAddresses(bccAddresses);
		replyToAddresses = copyAddresses(replyToAddresses);

		if (toAddresses.isEmpty()) {
			throw new IllegalArgumentException("toAddresses must not be empty.");
		}
		if (!hasText(subject)) {
			throw new IllegalArgumentException("subject must not be blank.");
		}
		if (!hasText(textBody) && !hasText(htmlBody)) {
			throw new IllegalArgumentException("textBody or htmlBody must not be blank.");
		}
	}

	public static EmailMessage html(String toAddress, String subject, String textBody, String htmlBody) {
		return EmailMessage.builder()
			.toAddresses(List.of(toAddress))
			.subject(subject)
			.textBody(textBody)
			.htmlBody(htmlBody)
			.build();
	}

	public boolean hasTextBody() {
		return hasText(textBody);
	}

	public boolean hasHtmlBody() {
		return hasText(htmlBody);
	}

	private static List<String> copyAddresses(List<String> addresses) {
		if (addresses == null) {
			return List.of();
		}
		return addresses.stream()
			.filter(EmailMessage::hasText)
			.map(String::trim)
			.toList();
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
