package com.coniv.mait.global.jwt;

import lombok.Builder;
import lombok.Getter;

@Getter
public class Token {
	private String accessToken;
	private String refreshToken;

	@Builder
	public Token(String accessToken, String refreshToken) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}
}
