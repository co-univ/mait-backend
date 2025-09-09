package com.coniv.mait.global.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "mait.cookie")
@Getter
@Setter
public class CookieProperty {

	private String domain;
	private String path = "/";
	private long maxAge = 60 * 60 * 24 * 3; // 3 days
	private boolean httpOnly = true;
	private boolean secure = true;
	private String sameSite = "Lax";
}
