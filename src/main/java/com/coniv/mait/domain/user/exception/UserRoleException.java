package com.coniv.mait.domain.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserRoleException extends RuntimeException {
	private final String message;
}
