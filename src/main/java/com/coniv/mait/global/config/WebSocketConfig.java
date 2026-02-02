package com.coniv.mait.global.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.coniv.mait.global.interceptor.HttpHandshakeInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final List<ChannelInterceptor> channelInterceptors;
	private final HttpHandshakeInterceptor httpHandshakeInterceptor;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// 메시지 브로커 설정 - "/topic"으로 시작하는 메시지는 브로커가 처리
		config.enableSimpleBroker("/topic");

		// 클라이언트에서 메시지를 보낼 때 사용할 prefix
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(channelInterceptors.toArray(new ChannelInterceptor[0]));
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// WebSocket 연결을 위한 엔드포인트 설정
		registry.addEndpoint("/ws")
			.setAllowedOriginPatterns("*")  // CORS 설정: 모든 출처에서의 연결 허용
			.addInterceptors(httpHandshakeInterceptor) // HTTP 핸드셰이크 시 request info(헤더 등)를 가져오기 위한 인터셉터
			.withSockJS();  // SockJS fallback 옵션
	}
}
