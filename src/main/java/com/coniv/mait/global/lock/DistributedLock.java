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

	// 락 보유 시간. 음수면 watchdog 이 점유를 자동 갱신해 작업이 끝날 때까지 유지한다.
	// 양수로 고정하면 작업이 끝나지 않아도 만료되어 상호배제가 깨지므로 필요한 곳에서만 지정한다.
	long leaseTime() default -1L;

	TimeUnit timeUnit() default TimeUnit.SECONDS;
}
