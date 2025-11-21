package com.coniv.mait.global.oauth.constant;

import org.springframework.stereotype.Component;

import com.coniv.mait.global.config.property.MaitProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Component
@RequiredArgsConstructor
public final class AuthConstant {

	private final MaitProperty maitProperty;

	public String getOAuthSuccessRedirectUrl() {
		return maitProperty.getBaseUrl() + "/oauth/success";
	}

	public String getOauthSignupUrl() {
		// return maitProperty.getBaseUrl() + "/oauth/signup"; //TODO 배포 후 수정
		return "http://localhost:3000/oauth/signup";
	}
}
