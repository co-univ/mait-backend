package com.coniv.mait.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.coniv.mait.global.config.property.MaitProperty;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

	private final MaitProperty maitProperty;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.addAllowedOrigin(maitProperty.getBaseUrl());

		configuration.addAllowedOriginPattern("*");
		configuration.addAllowedMethod("*");
		configuration.addAllowedHeader("*");
		configuration.setAllowCredentials(true);
		configuration.addExposedHeader("*");

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
