package com.coniv.mait.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.coniv.mait.global.config.property.AwsCredentialsProperty;
import com.coniv.mait.global.config.property.EmailProperty;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mait.email", name = "provider", havingValue = "ses", matchIfMissing = true)
public class SesConfig {

	private final AwsCredentialsProperty awsCredentialsProperty;

	private final EmailProperty emailProperty;

	@Value("${spring.cloud.aws.region.static:ap-northeast-2}")
	private String region;

	@Bean
	public SesV2Client sesV2Client() {
		return SesV2Client.builder()
			.region(Region.of(region))
			.credentialsProvider(credentialsProvider())
			.build();
	}

	private AwsCredentialsProvider credentialsProvider() {
		EmailProperty.Credentials sesCredentials = emailProperty.getCredentials();
		if (hasText(sesCredentials.getAccessKey()) && hasText(sesCredentials.getSecretKey())) {
			return StaticCredentialsProvider.create(
				AwsBasicCredentials.create(sesCredentials.getAccessKey(), sesCredentials.getSecretKey())
			);
		}

		if (hasText(awsCredentialsProperty.getAccessKey()) && hasText(awsCredentialsProperty.getSecretKey())) {
			return StaticCredentialsProvider.create(
				AwsBasicCredentials.create(awsCredentialsProperty.getAccessKey(), awsCredentialsProperty.getSecretKey())
			);
		}

		return DefaultCredentialsProvider.create();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
