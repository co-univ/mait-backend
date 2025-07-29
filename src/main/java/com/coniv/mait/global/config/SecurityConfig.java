package com.coniv.mait.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import com.coniv.mait.domain.auth.service.Oauth2UserService;
import com.coniv.mait.global.security.OAuth2SuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final Oauth2UserService oauth2UserService;
	private final OAuth2SuccessHandler oauth2SuccessHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> {
			})
			.authorizeHttpRequests(auth -> auth
				.anyRequest().permitAll()) // 임시로 모든 요청 허용 TODO: 실제 서비스에서는 적절한 권한 설정 필요
			.oauth2Login((oauth2) -> oauth2
				.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
					.userService(oauth2UserService)
				)
				.successHandler(oauth2SuccessHandler)
			);
		return httpSecurity.build();
	}
}
