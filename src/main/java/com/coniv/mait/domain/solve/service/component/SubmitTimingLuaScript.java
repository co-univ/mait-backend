package com.coniv.mait.domain.solve.service.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SubmitTimingLuaScript {

	public static final String SUBMIT_TIMING_LUA_SCRIPT =
		"-- KEYS[1]: $submit:order:questionId:{문제ID}\n"
			+ "-- KEYS[2]: $submit:first-time:questionId:{문제ID}\n"
			+ "-- return : '제출순번:첫제출과의 시간차(ms)'\n"
			+ "\n"
			+ "local orderKey = KEYS[1]\n"
			+ "local firstTimeKey = KEYS[2]\n"
			+ "\n"
			+ "local order = redis.call('INCR', orderKey)\n"
			+ "\n"
			+ "local t = redis.call('TIME')\n"
			+ "local nowMs = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)\n"
			+ "\n"
			+ "local first = redis.call('GET', firstTimeKey)\n"
			+ "if order == 1 or not first then\n"
			+ "  redis.call('SET', firstTimeKey, nowMs)\n"
			+ "  return order .. ':0'\n"
			+ "end\n"
			+ "\n"
			+ "return order .. ':' .. (nowMs - tonumber(first))";
}
