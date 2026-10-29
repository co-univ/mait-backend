package com.coniv.mait.global.interceptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.jwt.JwtTokenProvider;
import com.coniv.mait.global.auth.jwt.Token;
import com.coniv.mait.global.auth.jwt.repository.BlackListRepository;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

	@Mock
	private BlackListRepository blackListRepository;

	@Mock
	private UserEntityRepository userEntityRepository;

	private JwtTokenProvider jwtTokenProvider;

	private WebSocketAuthInterceptor webSocketAuthInterceptor;

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider(blackListRepository);
		ReflectionTestUtils.setField(jwtTokenProvider, "secretKey",
			"test-secret-key-for-testing-purposes-only-minimum-256-bits-required");
		ReflectionTestUtils.setField(jwtTokenProvider, "accessExpiration", 3_600_000L);
		ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", 604_800_000L);
		webSocketAuthInterceptor = new WebSocketAuthInterceptor(jwtTokenProvider, userEntityRepository);
	}

	@Test
	@DisplayName("access token으로 CONNECT 하면 사용자를 인증한다")
	void preSend_AccessToken_SetsUser() {
		// given
		Long userId = 1L;
		Token token = jwtTokenProvider.createToken(userId);
		given(blackListRepository.existsById(token.accessToken())).willReturn(false);
		given(userEntityRepository.existsById(userId)).willReturn(true);
		StompHeaderAccessor accessor = connectAccessor(token.accessToken());

		// when
		webSocketAuthInterceptor.preSend(toMessage(accessor), mock(MessageChannel.class));

		// then
		assertThat(accessor.getUser()).isNotNull();
		assertThat(accessor.getUser().getName()).isEqualTo(String.valueOf(userId));
	}

	@Test
	@DisplayName("refresh token으로 CONNECT 하면 사용자를 인증하지 않는다")
	void preSend_RefreshToken_DoesNotSetUser() {
		// given
		Token token = jwtTokenProvider.createToken(1L);
		StompHeaderAccessor accessor = connectAccessor(token.refreshToken());

		// when
		webSocketAuthInterceptor.preSend(toMessage(accessor), mock(MessageChannel.class));

		// then
		assertThat(accessor.getUser()).isNull();
		then(userEntityRepository).shouldHaveNoInteractions();
	}

	private StompHeaderAccessor connectAccessor(String token) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setNativeHeader("Authorization", token);
		accessor.setLeaveMutable(true);
		return accessor;
	}

	private Message<byte[]> toMessage(StompHeaderAccessor accessor) {
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}
}
