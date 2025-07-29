package com.coniv.mait.global.util;

import static com.coniv.mait.global.jwt.constant.TokenConstants.*;

import jakarta.servlet.http.Cookie;

public final class CookieUtil {

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
