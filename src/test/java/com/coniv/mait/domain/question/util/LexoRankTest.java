package com.coniv.mait.domain.question.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LexoRankTest {

	@Test
	@DisplayName("prev와 next가 모두 null이면 middle 키를 반환한다")
	void bothNull_returnsMiddle() {
		String key = LexoRank.between(null, null);
		assertThat(key).isNotBlank();
	}

	@Test
	@DisplayName("같은 자릿수에서 간단한 중간 키를 생성한다")
	void between_simple() {
		String key = LexoRank.between("A", "C");
		assertThat(key).isBetween("A", "C");
		assertThat(key).startsWith("B");
	}

	@Test
	@DisplayName("인접한 키 사이에서는 더 깊은 자릿수로 확장한다")
	void between_adjacent_expandsDepth() {
		String key = LexoRank.between("A", "B");
		assertThat(key).startsWith("A");
		assertThat(key).isGreaterThan("A");
		assertThat(key).isLessThan("B");
	}

	@Test
	@DisplayName("nextAfter는 더 큰 키를 생성한다")
	void nextAfter_basic() {
		String next = LexoRank.nextAfter("A");
		assertThat(next).isGreaterThan("A");
	}

	@Test
	@DisplayName("prevBefore는 더 작은 키를 생성한다")
	void prevBefore_basic() {
		String prev = LexoRank.prevBefore("B");
		assertThat(prev).isLessThan("B");
	}

	@Test
	@DisplayName("긴 공통 접두어를 가진 경우에도 between이 동작한다")
	void between_longCommonPrefix() {
		String key = LexoRank.between("Az", "B");
		assertThat(key).startsWith("A");
		assertThat(key).isGreaterThan("Az");
		assertThat(key).isLessThan("B");
	}
}

