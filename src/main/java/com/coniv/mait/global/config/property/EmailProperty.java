package com.coniv.mait.global.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mait.email")
public class EmailProperty {
	private String provider = "ses";
	private String fromAddress = "no-reply@mait.kr";
	private String replyToAddress;
	private String configurationSetName;
	private Credentials credentials = new Credentials();

	@Getter
	@Setter
	public static class Credentials {
		private String accessKey;
		private String secretKey;
	}
}
