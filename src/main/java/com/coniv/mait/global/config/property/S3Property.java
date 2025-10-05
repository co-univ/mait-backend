package com.coniv.mait.global.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.cloud.aws.s3")
public class S3Property {
	private String bucket;
}
