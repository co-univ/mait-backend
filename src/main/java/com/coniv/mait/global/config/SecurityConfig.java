package com.coniv.mait.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.coniv.mait.domain.auth.service.Oauth2UserService;
import com.coniv.mait.global.auth.jwt.JwtAuthenticationEntryPoint;
import com.coniv.mait.global.oauth.OAuth2SuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final Oauth2UserService oauth2UserService;
	private final OAuth2SuccessHandler oauth2SuccessHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> {
			})
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/v1/auth/login",
					"/api/v1/auth/reissue",
					"/api/v1/auth/access-token",
					"/oauth2/**",
					"/login/oauth2/**"
				).permitAll()
				.requestMatchers(
					"/api/v1/users/sign-up",
					"/api/v1/users/nickname/random"
				).permitAll()
				.requestMatchers("/api/v1/policies").permitAll()
				.requestMatchers("/api/v1/teams/invitation/info").permitAll()
				.requestMatchers(
					"/api-docs/**",
					"/swagger-ui/**",
					"/swagger-ui.html"
				).permitAll()
				.requestMatchers(
					"/actuator/health",
					"/actuator/health/**",
					"/actuator/prometheus"
				).permitAll()
				.requestMatchers(
					"/favicon.ico",
					"/error"
				).permitAll()
				.requestMatchers("/ws/**").permitAll()
				.anyRequest().authenticated()
			)
			.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.oauth2Login((oauth2) -> oauth2
				.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
					.userService(oauth2UserService)
				)
				.successHandler(oauth2SuccessHandler)
			);
		return httpSecurity.build();
	}
}
