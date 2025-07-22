package com.coniv.mait.domain.auth.oauth;

public interface OAuth2UserInfo {
	String getProvider();

	String getProviderId();

	String getEmail();

	String getName();
}
