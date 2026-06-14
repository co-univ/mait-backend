package com.coniv.mait.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.coniv.mait.global.interceptor.WebSocketAuthInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private static final long[] HEARTBEAT_INTERVALS = {10_000L, 10_000L};

	private final WebSocketAuthInterceptor webSocketAuthInterceptor;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// 메시지 브로커 설정 - 브로드캐스트는 "/topic", 개인 큐는 "/queue"로 처리
		config.enableSimpleBroker("/topic", "/queue")
			.setHeartbeatValue(HEARTBEAT_INTERVALS)
			.setTaskScheduler(webSocketHeartbeatScheduler());

		// 클라이언트에서 메시지를 보낼 때 사용할 prefix
		config.setApplicationDestinationPrefixes("/app");

		// "/user" prefix는 Spring user destination 규약을 사용한다.
		config.setUserDestinationPrefix("/user");
	}

	@Bean
	public TaskScheduler webSocketHeartbeatScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("ws-heartbeat-");
		scheduler.initialize();
		return scheduler;
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
