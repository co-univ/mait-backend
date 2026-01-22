package com.coniv.mait.global.auth.jwt;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@RedisHash(value = "blackList")
public class BlackList {

	private String id;

	@TimeToLive(unit = TimeUnit.DAYS)
	@Builder.Default
	private Long ttl = 14L;
}

