package com.coniv.mait.global.lock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.coniv.mait.global.exception.custom.DistributedLockException;

@ExtendWith(MockitoExtension.class)
class DistributedLockAspectTest {

	@Mock
	private RedissonClient redissonClient;

	@Mock
	private RLock lock;

	@Mock
	private ProceedingJoinPoint joinPoint;

	@Mock
	private MethodSignature methodSignature;

	@InjectMocks
	private DistributedLockAspect distributedLockAspect;

	static class LockTarget {

		@DistributedLock(key = "'copy:' + #userId + ':' + #questionSetId")
		public String copy(Long userId, Long questionSetId) {
			return "copied";
		}

		@DistributedLock(key = "'fixed'", waitTime = 3L, leaseTime = 30L, timeUnit = TimeUnit.MINUTES)
		public String withFixedLeaseTime(Long id) {
			return "ok";
		}
	}

	@AfterEach
	void clearTransactionSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	private void givenJoinPoint(String methodName, Class<?>[] parameterTypes, Object... args)
		throws NoSuchMethodException {
		Method method = LockTarget.class.getMethod(methodName, parameterTypes);
		given(joinPoint.getSignature()).willReturn(methodSignature);
		given(methodSignature.getMethod()).willReturn(method);
		given(joinPoint.getTarget()).willReturn(new LockTarget());
		given(joinPoint.getArgs()).willReturn(args);
	}

	private void givenWatchdogLockAcquired() throws InterruptedException {
		given(redissonClient.getLock(anyString())).willReturn(lock);
		given(lock.tryLock(anyLong(), any(TimeUnit.class))).willReturn(true);
	}

	@Test
	@DisplayName("락 획득에 성공하면 대상 메서드를 실행하고 락을 해제한다")
	void lockAcquired_proceedsAndUnlocks() throws Throwable {
		// given
		givenJoinPoint("copy", new Class<?>[] {Long.class, Long.class}, 1L, 2L);
		givenWatchdogLockAcquired();
		given(lock.isHeldByCurrentThread()).willReturn(true);
		given(joinPoint.proceed()).willReturn("copied");

		// when
		Object result = distributedLockAspect.lock(joinPoint);

		// then
		assertThat(result).isEqualTo("copied");
		verify(joinPoint).proceed();
		verify(lock).unlock();
	}

	@Test
	@DisplayName("SpEL 키가 파라미터 값으로 치환되고 락 키 prefix 가 붙는다")
	void lockKey_parsedFromSpelWithPrefix() throws Throwable {
		// given
		givenJoinPoint("copy", new Class<?>[] {Long.class, Long.class}, 7L, 99L);
		givenWatchdogLockAcquired();
		given(lock.isHeldByCurrentThread()).willReturn(true);
		given(joinPoint.proceed()).willReturn("copied");

		// when
		distributedLockAspect.lock(joinPoint);

		// then
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(redissonClient).getLock(keyCaptor.capture());
		assertThat(keyCaptor.getValue()).isEqualTo("$lock:copy:7:99");
	}

	@Test
	@DisplayName("점유 시간을 지정하지 않으면 watchdog 이 갱신하는 방식으로 락을 획득한다")
	void leaseTimeNotSpecified_acquiresWithWatchdog() throws Throwable {
		// given
		givenJoinPoint("copy", new Class<?>[] {Long.class, Long.class}, 1L, 2L);
		givenWatchdogLockAcquired();
		given(lock.isHeldByCurrentThread()).willReturn(true);
		given(joinPoint.proceed()).willReturn("copied");

		// when
		distributedLockAspect.lock(joinPoint);

		// then
		verify(lock).tryLock(0L, TimeUnit.SECONDS);
		verify(lock, never()).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
	}

	@Test
	@DisplayName("점유 시간을 지정하면 고정 만료 방식으로 락을 획득한다")
	void leaseTimeSpecified_acquiresWithFixedExpiry() throws Throwable {
		// given
		givenJoinPoint("withFixedLeaseTime", new Class<?>[] {Long.class}, 1L);
		given(redissonClient.getLock(anyString())).willReturn(lock);
		given(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
		given(lock.isHeldByCurrentThread()).willReturn(true);
		given(joinPoint.proceed()).willReturn("ok");

		// when
		distributedLockAspect.lock(joinPoint);

		// then
		verify(lock).tryLock(3L, 30L, TimeUnit.MINUTES);
		verify(lock, never()).tryLock(anyLong(), any(TimeUnit.class));
	}

	@Test
	@DisplayName("호출자 트랜잭션에 참여하면 메서드 반환 시점에 락을 해제하지 않는다")
	void joinedCallerTransaction_unlockDeferredUntilCompletion() throws Throwable {
		// given
		givenJoinPoint("copy", new Class<?>[] {Long.class, Long.class}, 1L, 2L);
		givenWatchdogLockAcquired();
		given(joinPoint.proceed()).willReturn("copied");

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);

		// when
		distributedLockAspect.lock(joinPoint);

		// then
		verify(lock, never()).unlock();
		assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
	}

	@Test
	@DisplayName("지연된 해제는 트랜잭션 종료 시점에 수행된다")
	void deferredUnlock_executedOnTransactionCompletion() throws Throwable {
		// given
		givenJoinPoint("copy", new Class<?>[] {Long.class, Long.class}, 1L, 2L);
		givenWatchdogLockAcquired();
		given(lock.isHeldByCurrentThread()).willReturn(true);
		given(joinPoint.proceed()).willReturn("copied");

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		distributedLockAspect.lock(joinPoint);

		// when
		TransactionSynchronizationManager.getSynchronizations()
			.forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

		// then
		verify(lock).unlock();
	}

	@Test
	@DisplayName("락 획득에 실패하면 예외가 발생하고 대상 메서드를 실행하지 않는다")
	void lockAcquisitionFailed_throwsAndDoesNotProceed() throws Throwable {
		// given
		givenJoinPoint("copy", new Class<?>[] {Long.class, Long.class}, 1L, 2L);
		given(redissonClient.getLock(anyString())).willReturn(lock);
		given(lock.tryLock(anyLong(), any(TimeUnit.class))).willReturn(false);

		// when & then
		assertThatThrownBy(() -> distributedLockAspect.lock(joinPoint))
			.isInstanceOf(DistributedLockException.class)
			.hasMessageContaining("$lock:copy:1:2");

		verify(joinPoint, never()).proceed();
		verify(lock, never()).unlock();
	}

	@Test
	@DisplayName("대상 메서드가 예외를 던져도 락은 해제된다")
	void targetThrows_lockReleased() throws Throwable {
		// given
		givenJoinPoint("copy", new Class<?>[] {Long.class, Long.class}, 1L, 2L);
		givenWatchdogLockAcquired();
		given(lock.isHeldByCurrentThread()).willReturn(true);
		given(joinPoint.proceed()).willThrow(new IllegalStateException("대상 메서드 실패"));

		// when & then
		assertThatThrownBy(() -> distributedLockAspect.lock(joinPoint))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("대상 메서드 실패");

		verify(lock).unlock();
	}

	@Test
	@DisplayName("락 대기 중 인터럽트가 발생하면 인터럽트 플래그를 복원하고 예외로 변환한다")
	void interrupted_restoresInterruptFlagAndThrows() throws Throwable {
		// given
		givenJoinPoint("copy", new Class<?>[] {Long.class, Long.class}, 1L, 2L);
		given(redissonClient.getLock(anyString())).willReturn(lock);
		given(lock.tryLock(anyLong(), any(TimeUnit.class))).willThrow(new InterruptedException());

		// when & then
		assertThatThrownBy(() -> distributedLockAspect.lock(joinPoint))
			.isInstanceOf(DistributedLockException.class);

		// 다른 테스트에 영향을 주지 않도록 플래그를 확인하며 초기화한다
		assertThat(Thread.interrupted()).isTrue();
		verify(joinPoint, never()).proceed();
		verify(lock, never()).unlock();
	}
}
