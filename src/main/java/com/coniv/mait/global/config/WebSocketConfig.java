package com.coniv.mait.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// 메시지 브로커 설정 - "/topic"으로 시작하는 메시지는 브로커가 처리
		config.enableSimpleBroker("/topic");

		// 클라이언트에서 메시지를 보낼 때 사용할 prefix
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// WebSocket 연결을 위한 엔드포인트 설정
		registry.addEndpoint("/ws")
			.setAllowedOrigins(
				"http://localhost:3000",           // 로컬 개발 환경
				"https://localhost:3000",          // 로컬 HTTPS 개발 환경
				"https://dev.mait.kr",             // 개발 프론트엔드
				"https://mait.kr"                  // 프로덕션 프론트엔드
			)  // 프론트엔드 도메인들 허용
			.withSockJS();  // SockJS fallback 옵션
	}
}
