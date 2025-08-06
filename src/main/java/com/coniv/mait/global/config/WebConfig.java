package com.coniv.mait.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final IdempotencyInterceptor idempotencyInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(idempotencyInterceptor)
			.addPathPatterns("/api/v1/question-sets/*/questions/*/submit");
	}
}
