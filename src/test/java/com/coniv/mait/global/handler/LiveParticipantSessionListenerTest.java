package com.coniv.mait.global.handler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.security.Principal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import com.coniv.mait.domain.question.service.component.LiveParticipantRedisRepository;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;

@ExtendWith(MockitoExtension.class)
class LiveParticipantSessionListenerTest {

	@InjectMocks
	private LiveParticipantSessionListener liveParticipantSessionListener;

	@Mock
	private LiveParticipantRedisRepository liveParticipantRedisRepository;

	@Mock
	private QuestionWebSocketSender questionWebSocketSender;

	@Test
	@DisplayName("participate 토픽 구독 시 참가자 수가 변하면 현재 인원을 전파한다")
	void onSubscribe_participantChanged_broadcastsCount() {
		given(liveParticipantRedisRepository.enter(7L, 42L, "sess-A", "sub-0")).willReturn(true);
		given(liveParticipantRedisRepository.getParticipantCount(7L)).willReturn(5L);

		liveParticipantSessionListener.onSubscribe(
			subscribeEvent("/topic/question-sets/7/participate", "sess-A", "sub-0", token(42L)));

		then(questionWebSocketSender).should().broadcastParticipantCount(7L, 5L);
	}

	@Test
	@DisplayName("participate 토픽 구독이지만 참가자 수가 그대로면 전파하지 않는다")
	void onSubscribe_participantNotChanged_doesNotBroadcast() {
		given(liveParticipantRedisRepository.enter(7L, 42L, "sess-A", "sub-0")).willReturn(false);

		liveParticipantSessionListener.onSubscribe(
			subscribeEvent("/topic/question-sets/7/participate", "sess-A", "sub-0", token(42L)));

		then(questionWebSocketSender).shouldHaveNoInteractions();
		then(liveParticipantRedisRepository).should(never()).getParticipantCount(anyLong());
	}

	@Test
	@DisplayName("participate 토픽이 아니면 입장 처리를 하지 않는다")
	void onSubscribe_notParticipateTopic_ignored() {
		liveParticipantSessionListener.onSubscribe(
			subscribeEvent("/topic/question-sets/7/manage", "sess-A", "sub-0", token(42L)));

		then(liveParticipantRedisRepository).shouldHaveNoInteractions();
		then(questionWebSocketSender).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("인증되지 않은 구독이면 입장 처리를 하지 않는다")
	void onSubscribe_unauthenticated_ignored() {
		liveParticipantSessionListener.onSubscribe(
			subscribeEvent("/topic/question-sets/7/participate", "sess-A", "sub-0", null));

		then(liveParticipantRedisRepository).shouldHaveNoInteractions();
		then(questionWebSocketSender).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("구독해제로 참가자가 빠지면 현재 인원을 전파한다")
	void onUnsubscribe_participantLeft_broadcastsCount() {
		given(liveParticipantRedisRepository.leaveBySubscription("sess-A", "sub-0")).willReturn(7L);
		given(liveParticipantRedisRepository.getParticipantCount(7L)).willReturn(4L);

		liveParticipantSessionListener.onUnsubscribe(unsubscribeEvent("sess-A", "sub-0"));

		then(questionWebSocketSender).should().broadcastParticipantCount(7L, 4L);
	}

	@Test
	@DisplayName("구독해제로 참가자 수 변화가 없으면 전파하지 않는다")
	void onUnsubscribe_noChange_doesNotBroadcast() {
		given(liveParticipantRedisRepository.leaveBySubscription("sess-A", "sub-0")).willReturn(null);

		liveParticipantSessionListener.onUnsubscribe(unsubscribeEvent("sess-A", "sub-0"));

		then(questionWebSocketSender).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("연결 종료로 참가자가 빠지면 현재 인원을 전파한다")
	void onDisconnect_participantLeft_broadcastsCount() {
		given(liveParticipantRedisRepository.leaveBySession("sess-A")).willReturn(7L);
		given(liveParticipantRedisRepository.getParticipantCount(7L)).willReturn(2L);

		liveParticipantSessionListener.onDisconnect(disconnectEvent("sess-A"));

		then(questionWebSocketSender).should().broadcastParticipantCount(7L, 2L);
	}

	@Test
	@DisplayName("연결 종료로 참가자 수 변화가 없으면 전파하지 않는다")
	void onDisconnect_noChange_doesNotBroadcast() {
		given(liveParticipantRedisRepository.leaveBySession("sess-A")).willReturn(null);

		liveParticipantSessionListener.onDisconnect(disconnectEvent("sess-A"));

		then(questionWebSocketSender).shouldHaveNoInteractions();
	}

	private Principal token(Long userId) {
		return new UsernamePasswordAuthenticationToken(userId, null, null);
	}

	private SessionSubscribeEvent subscribeEvent(String destination, String sessionId, String subscriptionId,
		Principal user) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination(destination);
		accessor.setSessionId(sessionId);
		accessor.setSubscriptionId(subscriptionId);
		if (user != null) {
			accessor.setUser(user);
		}
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
		return new SessionSubscribeEvent(this, message);
	}

	private SessionUnsubscribeEvent unsubscribeEvent(String sessionId, String subscriptionId) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
		accessor.setSessionId(sessionId);
		accessor.setSubscriptionId(subscriptionId);
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
		return new SessionUnsubscribeEvent(this, message);
	}

	private SessionDisconnectEvent disconnectEvent(String sessionId) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		accessor.setSessionId(sessionId);
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
		return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL);
	}
}
