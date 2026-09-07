package com.coniv.mait.global.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

	String key();

	// 락 획득을 기다리는 시간. 중복 요청 방지 용도는 대기 없이 즉시 거절한다.
	long waitTime() default 0L;

	// 락 보유 시간. 임계 구역 소요 시간보다 짧으면 자동 해제되어 상호배제가 깨진다.
	long leaseTime() default 10L;

	TimeUnit timeUnit() default TimeUnit.SECONDS;
}
