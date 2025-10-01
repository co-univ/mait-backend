package com.coniv.mait.global.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cloud.aws.credentials")
public class AwsCredentialsProperty {
	private String accessKey;
	private String secretKey;
}
