package com.coniv.mait.global.interceptor.idempotency;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.coniv.mait.global.config.RedisConfig;

@SpringBootTest(classes = {RedisConfig.class, IdempotencyRedisRepository.class, JacksonAutoConfiguration.class})
@ActiveProfiles("test")
class IdempotencyRedisRepositoryIntegrationTest {

	@Autowired
	private IdempotencyRedisRepository repository;

	@Autowired
	private StringRedisTemplate redis;

	private String key;
	private String redisKey;

	@BeforeEach
	void setUp() {
		key = "test:" + UUID.randomUUID();
		redisKey = "$Idempotency:v2:" + key;
	}

	@AfterEach
	void tearDown() {
		redis.delete(redisKey);
	}

	@Test
	@DisplayName("동시에 시작한 요청 중 하나만 원자적으로 선점한다")
	void simultaneousClaimsHaveSingleOwner() throws Exception {
		// given
		int count = 16;
		CountDownLatch ready = new CountDownLatch(count);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(count)) {
			List<Future<IdempotencyRecord>> claims = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				claims.add(executor.submit(() -> {
					IdempotencyRecord processing = IdempotencyRecord.processing("same-body");
					ready.countDown();
					if (!start.await(5, TimeUnit.SECONDS)) {
						throw new IllegalStateException("동시 시작 대기 시간 초과");
					}
					return repository.claim(key, processing) ? processing : null;
				}));
			}
			// when
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			List<IdempotencyRecord> owners = new ArrayList<>();
			for (Future<IdempotencyRecord> claim : claims) {
				IdempotencyRecord owner = claim.get(5, TimeUnit.SECONDS);
				if (owner != null) {
					owners.add(owner);
				}
			}
			// then
			assertThat(owners).hasSize(1);
			assertThat(repository.get(key)).isEqualTo(owners.getFirst());
		}
	}

	@Test
	@DisplayName("완료 상태와 HTTP 응답을 하나의 값으로 저장한다")
	void saveCompletedResponseStoresStatusAndResponseAtomically() {
		// given
		IdempotencyRecord owner = IdempotencyRecord.processing("body");
		repository.claim(key, owner);
		redis.expire(redisKey, Duration.ofSeconds(10));
		IdempotencyResponse response = new IdempotencyResponse(201,
			Map.of("Location", List.of("/result/1")), new byte[] {0, 1, 2});
		// when
		assertThat(repository.saveCompletedResponse(key, owner, response)).isTrue();
		// then
		IdempotencyRecord completed = repository.get(key);
		assertThat(completed.status()).isEqualTo(IdempotencyStatus.COMPLETED);
		assertThat(completed.response()).usingRecursiveComparison().isEqualTo(response);
		assertThat(redis.getExpire(redisKey)).isBetween(55L, 60L);
		assertThat(repository.release(key, owner)).isFalse();
		assertThat(repository.claim(key, IdempotencyRecord.processing("body"))).isFalse();
	}

	@Test
	@DisplayName("실패한 실행을 해제하면 동일한 키로 바로 재시도할 수 있다")
	void failedOwnerCanRetryImmediately() {
		IdempotencyRecord first = IdempotencyRecord.processing("body");
		assertThat(repository.claim(key, first)).isTrue();
		assertThat(repository.release(key, first)).isTrue();
		IdempotencyRecord retry = IdempotencyRecord.processing("body");
		assertThat(repository.claim(key, retry)).isTrue();
		assertThat(repository.get(key)).isEqualTo(retry);
	}

	@Test
	@DisplayName("만료된 소유자는 새 실행의 상태를 완료하거나 삭제할 수 없다")
	void expiredOwnerCannotModifyReplacement() {
		IdempotencyRecord old = IdempotencyRecord.processing("body");
		repository.claim(key, old);
		redis.expireAt(redisKey, Instant.EPOCH);
		IdempotencyRecord replacement = IdempotencyRecord.processing("body");
		assertThat(repository.claim(key, replacement)).isTrue();
		assertThat(repository.saveCompletedResponse(key, old, new IdempotencyResponse(200, Map.of(), new byte[0])))
			.isFalse();
		assertThat(repository.release(key, old)).isFalse();
		assertThat(repository.get(key)).isEqualTo(replacement);
	}

	@Test
	@DisplayName("처리 중 상태는 60초 보관하고 조회할 때 만료 시간을 연장하지 않는다")
	void processingHasFixedTtl() {
		// given
		IdempotencyRecord owner = IdempotencyRecord.processing("body");
		// when
		assertThat(repository.claim(key, owner)).isTrue();
		// then
		assertThat(redis.getExpire(redisKey)).isBetween(55L, 60L);
		redis.expire(redisKey, Duration.ofSeconds(10));
		assertThat(repository.get(key)).isEqualTo(owner);
		assertThat(redis.getExpire(redisKey)).isBetween(5L, 10L);
	}

	@Test
	@DisplayName("완료 응답 보관 기간이 끝나면 같은 키로 다시 선점할 수 있다")
	void expiredResponseCanBeClaimedAgain() {
		// given
		IdempotencyRecord owner = IdempotencyRecord.processing("body");
		repository.claim(key, owner);
		assertThat(repository.saveCompletedResponse(key, owner, new IdempotencyResponse(200, Map.of(), new byte[0])))
			.isTrue();
		// when
		redis.expireAt(redisKey, Instant.EPOCH);
		// then
		assertThat(repository.get(key)).isNull();
		IdempotencyRecord retry = IdempotencyRecord.processing("body");
		assertThat(repository.claim(key, retry)).isTrue();
		assertThat(repository.get(key)).isEqualTo(retry);
	}
}
