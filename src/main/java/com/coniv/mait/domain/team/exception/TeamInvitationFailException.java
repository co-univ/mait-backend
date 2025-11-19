package com.coniv.mait.domain.team.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeamInvitationFailException extends RuntimeException {
	private final InvitationErrorCode errorCode;
}
