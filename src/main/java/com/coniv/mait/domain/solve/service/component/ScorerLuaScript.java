package com.coniv.mait.domain.solve.service.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScorerLuaScript {

	public static final String SCORER_CHECK_LUA_SCRIPT =
		"-- KEYS[1]: score:{문제ID}\n"
			+ "-- ARGV[1]: userId\n"
			+ "-- ARGV[2]: order (Redis INCR로 받은 순번, 숫자형)\n"
			+ "\n"
			+ "local key = KEYS[1]\n"
			+ "local userId = ARGV[1]\n"
			+ "local order = tonumber(ARGV[2])\n"
			+ "\n"
			+ "local prev = redis.call('GET', key)\n"
			+ "if not prev then\n"
			+ "  redis.call('SET', key, userId .. ':' .. order)\n"
			+ "  return userId\n"
			+ "else\n"
			+ "  local prevOrder = tonumber(string.match(prev, ':(%d+)$'))\n"
			+ "  if order < prevOrder then\n"
			+ "    redis.call('SET', key, userId .. ':' .. order)\n"
			+ "    return userId\n"
			+ "  else\n"
			+ "    return string.match(prev, '^(.-):')\n"
			+ "  end\n"
			+ "end";

}
