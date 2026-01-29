package com.coniv.mait.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.addAllowedOrigin("https://dev.mait.kr");
		configuration.addAllowedOrigin("https://alpha.mait.kr");
		configuration.addAllowedOrigin("https://mait.kr");
		configuration.addAllowedOrigin("https://www.mait.kr");

		configuration.addAllowedOrigin("http://localhost:3000"); // todo 삭제 예정

		configuration.addAllowedMethod("*");
		configuration.addAllowedHeader("*");
		configuration.setAllowCredentials(true);
		configuration.addExposedHeader("*");

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
