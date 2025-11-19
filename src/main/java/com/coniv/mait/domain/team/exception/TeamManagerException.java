package com.coniv.mait.domain.team.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeamManagerException extends RuntimeException {
	private final String message;
}
