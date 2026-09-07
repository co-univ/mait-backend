package com.coniv.mait.global.lock;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.coniv.mait.global.exception.custom.DistributedLockException;
import com.coniv.mait.web.integration.BaseIntegrationTest;

@Import(DistributedLockIntegrationTest.LockTestConfig.class)
class DistributedLockIntegrationTest extends BaseIntegrationTest {

	private static final String KEY_PREFIX = "$lock:integration:";

	@Autowired
	private LockTestService lockTestService;

	@Autowired
	private RedissonClient redissonClient;

	@TestConfiguration
	static class LockTestConfig {

		@Bean
		LockTestService lockTestService() {
			return new LockTestService();
		}
	}

	static class LockTestService {

		@DistributedLock(key = "'integration:' + #id")
		public String run(Long id) {
			return "done:" + id;
		}

		@DistributedLock(key = "'integration:' + #id")
		public String runAndThrow(Long id) {
			throw new IllegalStateException("대상 메서드 실패");
		}
	}

	private ExecutorService holdLock(String lockKey, CountDownLatch acquired, CountDownLatch release) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			RLock lock = redissonClient.getLock(lockKey);
			lock.lock(10, TimeUnit.SECONDS);
			acquired.countDown();
			try {
				release.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			} finally {
				if (lock.isHeldByCurrentThread()) {
					lock.unlock();
				}
			}
		});
		return executor;
	}

	@Test
	@DisplayName("어노테이션이 붙은 메서드가 프록시를 통해 정상 실행된다")
	void annotatedMethod_executesThroughProxy() {
		// when
		String result = lockTestService.run(1L);

		// then
		assertThat(result).isEqualTo("done:1");
	}

	@Test
	@DisplayName("다른 스레드가 같은 키의 락을 점유 중이면 대기 없이 예외가 발생한다")
	void lockHeldByAnotherThread_throwsWithoutWaiting() throws Exception {
		// given
		final Long id = 2L;
		CountDownLatch acquired = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		ExecutorService executor = holdLock(KEY_PREFIX + id, acquired, release);

		try {
			assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

			// when & then
			assertThatThrownBy(() -> lockTestService.run(id))
				.isInstanceOf(DistributedLockException.class);
		} finally {
			release.countDown();
			executor.shutdown();
			assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
		}
	}

	@Test
	@DisplayName("락 키가 다르면 서로 차단하지 않는다")
	void differentLockKey_notBlocked() throws Exception {
		// given
		CountDownLatch acquired = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		ExecutorService executor = holdLock(KEY_PREFIX + 3L, acquired, release);

		try {
			assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

			// when
			String result = lockTestService.run(4L);

			// then
			assertThat(result).isEqualTo("done:4");
		} finally {
			release.countDown();
			executor.shutdown();
			assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
		}
	}

	@Test
	@DisplayName("대상 메서드가 예외로 끝나도 락이 해제되어 다음 요청이 처리된다")
	void targetThrows_lockReleasedForNextCall() {
		// given
		final Long id = 5L;
		assertThatThrownBy(() -> lockTestService.runAndThrow(id))
			.isInstanceOf(IllegalStateException.class);

		// when
		String result = lockTestService.run(id);

		// then
		assertThat(result).isEqualTo("done:5");
	}
}
