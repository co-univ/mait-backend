package com.coniv.mait.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.coniv.mait.domain.auth.service.Oauth2UserService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final Oauth2UserService oauth2UserService;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.oauth2Login((oauth2) -> oauth2
			.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
				.userService(oauth2UserService))
		);
		return httpSecurity.build();
	}
}
