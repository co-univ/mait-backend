package com.coniv.mait.domain.question.service.component;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LiveParticipantRedisRepository {

	private static final Duration SESSION_TTL = Duration.ofHours(12);
	private static final String VALUE_DELIMITER = ":";
	private static final int VALUE_PARTS = 3;

	private final StringRedisTemplate redisTemplate;

	public void enter(final Long questionSetId, final Long userId, final String sessionId,
		final String subscriptionId) {
		String sessionValue = questionSetId + VALUE_DELIMITER + userId + VALUE_DELIMITER + subscriptionId;
		redisTemplate.opsForValue().set(QuestionRedisKeys.liveSession(sessionId), sessionValue, SESSION_TTL);

		String userSessionsKey = QuestionRedisKeys.liveUserSessions(questionSetId, userId);
		redisTemplate.opsForSet().add(userSessionsKey, sessionId);
		redisTemplate.expire(userSessionsKey, SESSION_TTL);

		redisTemplate.opsForSet().add(QuestionRedisKeys.liveParticipants(questionSetId), String.valueOf(userId));
	}

	public void leaveBySubscription(final String sessionId, final String subscriptionId) {
		SessionInfo info = readSession(sessionId);
		if (info == null || !info.subscriptionId().equals(subscriptionId)) {
			return;
		}
		doLeave(sessionId, info);
		redisTemplate.delete(QuestionRedisKeys.liveSession(sessionId));
	}

	public void leaveBySession(final String sessionId) {
		SessionInfo info = readSession(sessionId);
		if (info == null) {
			return;
		}
		doLeave(sessionId, info);
		redisTemplate.delete(QuestionRedisKeys.liveSession(sessionId));
	}

	public long getParticipantCount(final Long questionSetId) {
		Long size = redisTemplate.opsForSet().size(QuestionRedisKeys.liveParticipants(questionSetId));
		return size == null ? 0L : size;
	}

	private void doLeave(final String sessionId, final SessionInfo info) {
		String userSessionsKey = QuestionRedisKeys.liveUserSessions(info.questionSetId(), info.userId());
		Long removed = redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
		if (removed == null || removed == 0L) {
			return;
		}

		Long remaining = redisTemplate.opsForSet().size(userSessionsKey);
		if (remaining == null || remaining == 0L) {
			redisTemplate.opsForSet().remove(
				QuestionRedisKeys.liveParticipants(info.questionSetId()), String.valueOf(info.userId()));
		}
	}

	private SessionInfo readSession(final String sessionId) {
		String value = redisTemplate.opsForValue().get(QuestionRedisKeys.liveSession(sessionId));
		if (value == null) {
			return null;
		}
		String[] parts = value.split(VALUE_DELIMITER, VALUE_PARTS);
		if (parts.length != VALUE_PARTS) {
			log.warn("[세션 매핑 파싱 실패] sessionId={} value={}", sessionId, value);
			return null;
		}
		return new SessionInfo(Long.valueOf(parts[0]), Long.valueOf(parts[1]), parts[2]);
	}

	private record SessionInfo(Long questionSetId, Long userId, String subscriptionId) {
	}
}
