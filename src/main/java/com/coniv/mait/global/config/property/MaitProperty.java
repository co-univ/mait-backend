package com.coniv.mait.global.config.property;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Setter
@Component
@ConfigurationProperties(prefix = "mait")
public class MaitProperty {

	private List<String> baseUrl;

	public List<String> getBaseUrls() {
		return baseUrl;
	}

	public String getBaseUrl() {
		return baseUrl.get(0);
	}
}
