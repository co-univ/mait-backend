package com.coniv.mait.global.util;

import static com.coniv.mait.global.jwt.constant.TokenConstants.*;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.coniv.mait.global.config.property.CookieProperty;

import jakarta.servlet.http.Cookie;
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

	private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 3; // 3 days

	public static Cookie createRefreshCookie(final String refreshToken) {
		Cookie cookie = new Cookie(REFRESH_TOKEN, refreshToken);
		cookie.setPath("/");
		cookie.setMaxAge(COOKIE_MAX_AGE);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);

		return cookie;
	}
}
