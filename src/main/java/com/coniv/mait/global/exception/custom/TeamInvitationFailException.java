package com.coniv.mait.global.exception.custom;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeamInvitationFailException extends RuntimeException {

	private final String message;
}
