package com.coniv.mait.global.component.dto;

public record EmailSendResult(String messageId) {

	public static EmailSendResult of(String messageId) {
		return new EmailSendResult(messageId);
	}
}
