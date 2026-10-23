package com.coniv.mait.global.interceptor.idempotency;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class IdempotencyRedisRepository {

	private static final String KEY_PREFIX = "$Idempotency:v2:";
	private static final Duration PROCESSING_TTL = Duration.ofSeconds(30);
	private static final Duration RESPONSE_TTL = Duration.ofHours(24);

	// 실행 소유자의 값과 일치할 때만 완료, 갱신 또는 삭제한다.
	private static final DefaultRedisScript<Long> UPDATE_SCRIPT = new DefaultRedisScript<>("""
		if redis.call('GET', KEYS[1]) ~= ARGV[1] then
			return 0
		end
		if ARGV[2] == '' then
			return redis.call('DEL', KEYS[1])
		end
		redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
		return 1
		""", Long.class);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final ScheduledExecutorService renewalExecutor;

	@Autowired
	public IdempotencyRedisRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this(redisTemplate, objectMapper, Executors.newSingleThreadScheduledExecutor(task -> {
			Thread thread = new Thread(task, "idempotency-renewal");
			thread.setDaemon(true);
			return thread;
		}));
	}

	IdempotencyRedisRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
		ScheduledExecutorService renewalExecutor) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.renewalExecutor = renewalExecutor;
	}

	public boolean claim(String key, IdempotencyRecord processing) {
		return Boolean.TRUE.equals(redisTemplate.opsForValue()
			.setIfAbsent(KEY_PREFIX + key, serialize(processing), PROCESSING_TTL));
	}

	public IdempotencyRecord get(String key) {
		String value = redisTemplate.opsForValue().get(KEY_PREFIX + key);
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.readValue(value, IdempotencyRecord.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("멱등성 상태를 읽을 수 없습니다.", exception);
		}
	}

	public boolean complete(String key, IdempotencyRecord processing, IdempotencyResponse response) {
		return update(key, processing, serialize(processing.completed(response)), RESPONSE_TTL);
	}

	public boolean release(String key, IdempotencyRecord processing) {
		return update(key, processing, "", PROCESSING_TTL);
	}

	public boolean renew(String key, IdempotencyRecord processing) {
		return update(key, processing, serialize(processing), PROCESSING_TTL);
	}

	public ScheduledFuture<?> startRenewal(String key, IdempotencyRecord processing) {
		return renewalExecutor.scheduleWithFixedDelay(() -> {
			try {
				if (!renew(key, processing)) {
					log.warn("멱등성 실행 소유권을 상실했습니다. key={}", key);
				}
			} catch (RuntimeException exception) {
				log.error("멱등성 실행 만료 시간 갱신에 실패했습니다. key={}", key, exception);
			}
		}, 10, 10, TimeUnit.SECONDS);
	}

	private boolean update(String key, IdempotencyRecord processing, String value, Duration ttl) {
		return Long.valueOf(1).equals(redisTemplate.execute(UPDATE_SCRIPT, List.of(KEY_PREFIX + key),
			serialize(processing), value, String.valueOf(ttl.toMillis())));
	}

	private String serialize(IdempotencyRecord record) {
		try {
			return objectMapper.writeValueAsString(record);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("멱등성 상태를 저장할 수 없습니다.", exception);
		}
	}

	@PreDestroy
	public void shutdown() {
		renewalExecutor.shutdownNow();
	}
}
