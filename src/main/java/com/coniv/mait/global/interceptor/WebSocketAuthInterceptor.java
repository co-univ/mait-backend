package com.coniv.mait.global.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.jwt.JwtTokenProvider;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";
	private final JwtTokenProvider jwtTokenProvider;
	private final UserEntityRepository userEntityRepository;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
			String token = extractToken(accessor);

			if (token != null) {
				try {
					authenticateUser(accessor, token);
				} catch (Exception e) {
					log.error("WebSocket authentication failed: {}", e.getMessage());
					// 인증 실패 시 null principal로 진행 (기존 방어 로직에서 처리됨)
				}
			} else {
				log.warn("WebSocket connection without authentication token");
			}
		}

		return message;
	}

	private String extractToken(StompHeaderAccessor accessor) {
		// 1. Authorization 헤더에서 추출 (표준 방식)
		String authHeader = accessor.getFirstNativeHeader("Authorization");
		if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
			return authHeader.substring(BEARER_PREFIX.length());
		}

		// 2. 쿼리 파라미터에서 추출 (일부 클라이언트 라이브러리용)
		String tokenParam = accessor.getFirstNativeHeader("token");
		if (tokenParam != null) {
			return tokenParam;
		}

		return null;
	}

	private void authenticateUser(StompHeaderAccessor accessor, String token) {
		jwtTokenProvider.validateAccessToken(token);
		Long userId = jwtTokenProvider.getUserId(token);

		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new JwtException("User not found with id: " + userId));

		// Spring Security의 Authentication 객체 생성 (HTTP 요청과 동일한 방식)
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(user, null, null);

		accessor.setUser(authentication);

		log.info("WebSocket authenticated: userId={}, userName={}", user.getId(), user.getName());
	}
}
