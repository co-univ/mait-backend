package com.coniv.mait.global.lock;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.coniv.mait.global.exception.custom.DistributedLockException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @Order 로 트랜잭션 어드바이스(LOWEST_PRECEDENCE)보다 바깥에서 동작시킨다.
// 락이 트랜잭션 안쪽에 들어가면 커밋 전에 해제되어 다음 스레드가 미커밋 상태를 읽는다.
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class DistributedLockAspect {

	private static final String LOCK_KEY_PREFIX = "$lock:";

	private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

	private final RedissonClient redissonClient;

	@Around("@annotation(com.coniv.mait.global.lock.DistributedLock)")
	public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
		final Method method = resolveTargetMethod(joinPoint);
		final DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
		final String lockKey = LOCK_KEY_PREFIX + parseKey(method, joinPoint.getArgs(), distributedLock.key());
		final RLock lock = redissonClient.getLock(lockKey);

		boolean acquired = false;
		try {
			acquired = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(),
				distributedLock.timeUnit());

			if (!acquired) {
				log.info("[분산락 획득 실패] key={}", lockKey);
				throw new DistributedLockException(lockKey);
			}

			return joinPoint.proceed();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			log.warn("[분산락 대기 중 인터럽트] key={}", lockKey);
			throw new DistributedLockException(lockKey);
		} finally {
			if (acquired && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	// JDK 프록시인 경우 시그니처가 인터페이스 메서드라 어노테이션을 찾지 못하므로 실제 대상 메서드로 보정한다.
	private Method resolveTargetMethod(final ProceedingJoinPoint joinPoint) {
		final Method method = ((MethodSignature)joinPoint.getSignature()).getMethod();
		final Object target = joinPoint.getTarget();

		if (target == null) {
			return method;
		}

		return AopUtils.getMostSpecificMethod(method, target.getClass());
	}

	private String parseKey(final Method method, final Object[] args, final String key) {
		final String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
		final EvaluationContext context = new StandardEvaluationContext();

		if (parameterNames != null) {
			for (int index = 0; index < parameterNames.length; index++) {
				context.setVariable(parameterNames[index], args[index]);
			}
		}

		return String.valueOf(EXPRESSION_PARSER.parseExpression(key).getValue(context));
	}
}
