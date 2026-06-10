package com.coniv.mait.domain.solve.service.component;

import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;

import com.coniv.mait.domain.solve.service.dto.SubmitTimingDto;

public class SubmitTimingResultSerializer implements RedisSerializer<SubmitTimingDto> {

	@Override
	public byte[] serialize(@Nullable SubmitTimingDto value) {
		return new byte[0];
	}

	@Override
	public SubmitTimingDto deserialize(@Nullable byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		return SubmitTimingDto.from(new String(bytes, StandardCharsets.UTF_8));
	}
}
