package com.coniv.mait.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.coniv.mait.global.interceptor.WebSocketAuthInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final WebSocketAuthInterceptor webSocketAuthInterceptor;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// 메시지 브로커 설정 - "/topic"으로 시작하는 메시지는 브로커가 처리
		config.enableSimpleBroker("/topic");

		// 클라이언트에서 메시지를 보낼 때 사용할 prefix
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		// WebSocket 인바운드 메시지 처리 전 JWT 인증 수행
		registration.interceptors(webSocketAuthInterceptor);
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// WebSocket 연결을 위한 엔드포인트 설정
		registry.addEndpoint("/ws")
			.setAllowedOriginPatterns("*")  // CORS 설정: 모든 출처에서의 연결 허용
			.withSockJS();  // SockJS fallback 옵션
	}
}
