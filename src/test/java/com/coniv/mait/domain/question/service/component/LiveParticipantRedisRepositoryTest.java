package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class LiveParticipantRedisRepositoryTest {

	@InjectMocks
	private LiveParticipantRedisRepository liveParticipantRedisRepository;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private SetOperations<String, String> setOperations;

	@Test
	@DisplayName("처음 입장한 유저면 참가자에 추가되고 true 를 반환한다")
	void enter_newUser_returnsTrue() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(redisTemplate.opsForSet()).willReturn(setOperations);
		given(setOperations.add(QuestionRedisKeys.liveUserSessions(7L, 42L), "sess-A")).willReturn(1L);
		given(setOperations.add(QuestionRedisKeys.liveParticipants(7L), "42")).willReturn(1L);

		boolean result = liveParticipantRedisRepository.enter(7L, 42L, "sess-A", "sub-0");

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("이미 참가 중인 유저의 추가 세션이면 false 를 반환한다")
	void enter_existingUser_returnsFalse() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(redisTemplate.opsForSet()).willReturn(setOperations);
		given(setOperations.add(QuestionRedisKeys.liveUserSessions(7L, 42L), "sess-B")).willReturn(1L);
		given(setOperations.add(QuestionRedisKeys.liveParticipants(7L), "42")).willReturn(0L);

		boolean result = liveParticipantRedisRepository.enter(7L, 42L, "sess-B", "sub-0");

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("마지막 세션이 끊기면 참가자에서 제거하고 questionSetId 를 반환한다")
	void leaveBySession_lastSession_removesParticipant() {
		String userSessionsKey = QuestionRedisKeys.liveUserSessions(7L, 42L);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(redisTemplate.opsForSet()).willReturn(setOperations);
		given(valueOperations.get(QuestionRedisKeys.liveSession("sess-A"))).willReturn("7:42:sub-0");
		given(setOperations.remove(userSessionsKey, "sess-A")).willReturn(1L);
		given(setOperations.size(userSessionsKey)).willReturn(0L);

		Long result = liveParticipantRedisRepository.leaveBySession("sess-A");

		assertThat(result).isEqualTo(7L);
		then(setOperations).should().remove(QuestionRedisKeys.liveParticipants(7L), "42");
	}

	@Test
	@DisplayName("세션이 끊겨도 같은 유저의 다른 세션이 남아있으면 참가자를 유지하고 null 을 반환한다")
	void leaveBySession_otherSessionRemains_keepsParticipant() {
		String userSessionsKey = QuestionRedisKeys.liveUserSessions(7L, 42L);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(redisTemplate.opsForSet()).willReturn(setOperations);
		given(valueOperations.get(QuestionRedisKeys.liveSession("sess-A"))).willReturn("7:42:sub-0");
		given(setOperations.remove(userSessionsKey, "sess-A")).willReturn(1L);
		given(setOperations.size(userSessionsKey)).willReturn(1L);

		Long result = liveParticipantRedisRepository.leaveBySession("sess-A");

		assertThat(result).isNull();
		then(setOperations).should(never()).remove(eq(QuestionRedisKeys.liveParticipants(7L)), anyString());
	}

	@Test
	@DisplayName("세션 매핑이 없으면 아무 처리 없이 null 을 반환한다")
	void leaveBySession_noMapping_returnsNull() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(QuestionRedisKeys.liveSession("sess-A"))).willReturn(null);

		Long result = liveParticipantRedisRepository.leaveBySession("sess-A");

		assertThat(result).isNull();
	}

	@Test
	@DisplayName("이미 제거된 세션이면 멱등하게 null 을 반환한다")
	void leaveBySession_alreadyRemoved_returnsNull() {
		String userSessionsKey = QuestionRedisKeys.liveUserSessions(7L, 42L);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(redisTemplate.opsForSet()).willReturn(setOperations);
		given(valueOperations.get(QuestionRedisKeys.liveSession("sess-A"))).willReturn("7:42:sub-0");
		given(setOperations.remove(userSessionsKey, "sess-A")).willReturn(0L);

		Long result = liveParticipantRedisRepository.leaveBySession("sess-A");

		assertThat(result).isNull();
		then(setOperations).should(never()).size(anyString());
	}

	@Test
	@DisplayName("잘못된 형식의 세션 매핑이면 null 을 반환한다")
	void leaveBySession_malformedMapping_returnsNull() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(QuestionRedisKeys.liveSession("sess-A"))).willReturn("malformed");

		Long result = liveParticipantRedisRepository.leaveBySession("sess-A");

		assertThat(result).isNull();
	}

	@Test
	@DisplayName("구독해제 시 subscriptionId 가 다르면 처리하지 않고 null 을 반환한다")
	void leaveBySubscription_differentSubscription_returnsNull() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(QuestionRedisKeys.liveSession("sess-A"))).willReturn("7:42:sub-0");

		Long result = liveParticipantRedisRepository.leaveBySubscription("sess-A", "sub-99");

		assertThat(result).isNull();
		then(setOperations).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("구독해제 시 subscriptionId 가 일치하고 마지막 세션이면 questionSetId 를 반환한다")
	void leaveBySubscription_matchingLastSession_returnsQuestionSetId() {
		String userSessionsKey = QuestionRedisKeys.liveUserSessions(7L, 42L);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(redisTemplate.opsForSet()).willReturn(setOperations);
		given(valueOperations.get(QuestionRedisKeys.liveSession("sess-A"))).willReturn("7:42:sub-0");
		given(setOperations.remove(userSessionsKey, "sess-A")).willReturn(1L);
		given(setOperations.size(userSessionsKey)).willReturn(0L);

		Long result = liveParticipantRedisRepository.leaveBySubscription("sess-A", "sub-0");

		assertThat(result).isEqualTo(7L);
	}

	@Test
	@DisplayName("참가자 수는 참가자 Set 의 크기를 반환한다")
	void getParticipantCount_returnsSetSize() {
		given(redisTemplate.opsForSet()).willReturn(setOperations);
		given(setOperations.size(QuestionRedisKeys.liveParticipants(7L))).willReturn(3L);

		assertThat(liveParticipantRedisRepository.getParticipantCount(7L)).isEqualTo(3L);
	}

	@Test
	@DisplayName("참가자 Set 이 비어있으면 0 을 반환한다")
	void getParticipantCount_nullSize_returnsZero() {
		given(redisTemplate.opsForSet()).willReturn(setOperations);
		given(setOperations.size(QuestionRedisKeys.liveParticipants(7L))).willReturn(null);

		assertThat(liveParticipantRedisRepository.getParticipantCount(7L)).isZero();
	}
}
