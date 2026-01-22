package com.coniv.mait.global.util;

import static com.coniv.mait.global.jwt.constant.TokenConstants.*;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.coniv.mait.global.config.property.CookieProperty;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class CookieUtil {

	private final CookieProperty cookieProperty;

	public ResponseCookie createRefreshResponseCookie(final String refreshToken) {
		return ResponseCookie.from(REFRESH_TOKEN, refreshToken)
			.domain(cookieProperty.getDomain())
			.path(cookieProperty.getPath())
			.maxAge(cookieProperty.getMaxAge())
			.httpOnly(cookieProperty.isHttpOnly())
			.secure(cookieProperty.isSecure())
			.sameSite(cookieProperty.getSameSite())
			.build();
	}

	public ResponseCookie createExpiredRefreshResponseCookie() {
		return ResponseCookie.from(REFRESH_TOKEN, "")
			.domain(cookieProperty.getDomain())
			.path(cookieProperty.getPath())
			.maxAge(0)
			.httpOnly(cookieProperty.isHttpOnly())
			.secure(cookieProperty.isSecure())
			.sameSite(cookieProperty.getSameSite())
			.build();
	}

	public ResponseCookie createOauthSignupCookie(final String signupKey) {
		return ResponseCookie.from(OAUTH_SIGNUP_KEY, signupKey)
			.domain(cookieProperty.getDomain())
			.path(cookieProperty.getPath())
			.maxAge(10 * 60)
			.httpOnly(cookieProperty.isHttpOnly())
			.secure(cookieProperty.isSecure())
			.sameSite(cookieProperty.getSameSite())
			.build();
	}
}
