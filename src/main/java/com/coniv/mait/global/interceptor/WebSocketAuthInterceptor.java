package com.coniv.mait.global.interceptor;

import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Order(value = 2)
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

	private static final String AUTH_HEADER = "Authorization";
	private final JwtTokenProvider jwtTokenProvider;
	private final UserEntityRepository userEntityRepository;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String token = accessor.getFirstNativeHeader(AUTH_HEADER);

			if (token != null) {
				try {
					authenticateUser(accessor, token);
				} catch (Exception e) {
					log.error("WebSocket authentication failed: {}", e.getMessage());
				}
			} else {
				log.warn("WebSocket connection without authentication token");
			}
		}

		return message;
	}

	private void authenticateUser(StompHeaderAccessor accessor, String token) {
		jwtTokenProvider.validateAccessToken(token);
		Long userId = jwtTokenProvider.getUserId(token);

		if (!userEntityRepository.existsById(userId)) {
			throw new JwtException("User not found with id: " + userId);
		}

		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userId, null, null);

		accessor.setUser(authentication);
		log.info("WebSocket authenticated: userId={}", userId);
	}
}
