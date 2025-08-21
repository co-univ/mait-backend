package com.coniv.mait.global.oauth.constant;

import org.springframework.beans.factory.annotation.Value;

public final class AuthConstant {

	@Value("${mait.front-base-url}")
	public static String frontendBaseUrl;

	public static final String OAUTH_SUCCESS_REDIRECT_URL = frontendBaseUrl + "/oauth/success";
}
