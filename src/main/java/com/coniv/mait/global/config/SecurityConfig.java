package com.coniv.mait.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.coniv.mait.domain.auth.service.Oauth2UserService;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.jwt.JwtAuthenticationEntryPoint;
import com.coniv.mait.global.oauth.OAuth2SuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final Oauth2UserService oauth2UserService;
	private final OAuth2SuccessHandler oauth2SuccessHandler;
	private final JwtAuthorizationFilter jwtAuthorizationFilter;

	private static final String[] WHITELIST = {
		"/login",
		"/ws/**",
		"/swagger-ui/**",
		"/swagger-ui.html",
		"/favicon.ico",
		"/v3/api-docs/**",
		"/api-docs/**",
		"/swagger-resources/**",
		"/webjars/**",
		"/api/v1/users/**",
		"/api/v1/auth/**",
		"/api/v1/policies/**"
	};

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> {
			})
			.formLogin(AbstractHttpConfigurer::disable) // 기본 폼 로그인 비활성화
			.httpBasic(AbstractHttpConfigurer::disable) // 기본 HTTP Basic 인증 비활성화
			.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(WHITELIST).permitAll()
				.anyRequest().authenticated()) // 나머지 요청은 인증 필요
			.addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
			.oauth2Login((oauth2) -> oauth2
				.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
					.userService(oauth2UserService)
				)
				.successHandler(oauth2SuccessHandler)
			);
		return httpSecurity.build();
	}
}
