package com.coniv.mait.domain.question.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LexoRank {

	private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final int BASE = ALPHABET.length();
	private static final int REBALANCED_WIDTH = 6;
	private static final int REBALANCE_LENGTH = 32;
	private static final Map<Character, Integer> CHAR_TO_INDEX = new HashMap<>();

	static {
		for (int i = 0; i < BASE; i++) {
			CHAR_TO_INDEX.put(ALPHABET.charAt(i), i);
		}
	}

	public static String middle() {
		return String.valueOf(ALPHABET.charAt(BASE / 2));
	}

	public static String between(String prev, String next) {
		if (prev == null && next == null) {
			return middle();
		}
		StringBuilder prefix = new StringBuilder();
		int index = 0;
		while (true) {
			int prevIndex = indexAt(prev, index, 0);
			int nextIndex = indexAt(next, index, BASE - 1);

			if (prevIndex == nextIndex) {
				prefix.append(ALPHABET.charAt(prevIndex));
				index++;
				continue;
			}

			if (prevIndex + 1 < nextIndex) {
				int mid = prevIndex + ((nextIndex - prevIndex) / 2);
				String candidate = prefix.append(ALPHABET.charAt(mid)).toString();
				if (next != null && candidate.compareTo(next) >= 0 && next.length() > 1) {
					String shorter = next.substring(0, next.length() - 1);
					if (prev == null || shorter.compareTo(prev) > 0) {
						return shorter;
					}
				}
				return candidate;
			}

			prefix.append(ALPHABET.charAt(prevIndex));
			index++;
		}
	}

	public static boolean needsRebalance(Collection<String> ranks) {
		Set<String> distinctRanks = new HashSet<>(ranks);
		if (distinctRanks.size() != ranks.size()) {
			return true;
		}
		for (String rank : distinctRanks) {
			if (rank == null || rank.isEmpty() || rank.length() > REBALANCE_LENGTH
				|| !rank.chars().allMatch(ch -> ALPHABET.indexOf(ch) >= 0)) {
				return true;
			}
			if (rank.equals("0")
				|| (rank.endsWith("0") && distinctRanks.contains(rank.substring(0, rank.length() - 1)))) {
				return true;
			}
		}
		return false;
	}

	public static String evenlySpaced(int index, int count) {
		if (index < 0 || index >= count) {
			throw new IllegalArgumentException("문제 인덱스가 재배치 범위를 벗어났습니다.");
		}
		long space = 1;
		for (int i = 0; i < REBALANCED_WIDTH; i++) {
			space *= BASE;
		}
		long position = (space / (count + 1L)) * (index + 1L);
		char[] rank = new char[REBALANCED_WIDTH];
		for (int i = rank.length - 1; i >= 0; i--) {
			rank[i] = ALPHABET.charAt((int)(position % BASE));
			position /= BASE;
		}
		return new String(rank);
	}

	public static String nextAfter(String rank) {
		return between(rank, null);
	}

	public static String prevBefore(String rank) {
		return between(null, rank);
	}

	private static int indexAt(String rank, int position, int missingDefault) {
		if (rank == null || position >= rank.length()) {
			return missingDefault;
		}
		Integer idx = CHAR_TO_INDEX.get(rank.charAt(position));
		if (idx == null) {
			throw new IllegalArgumentException("Invalid LexoRank character: " + rank.charAt(position));
		}
		return idx;
	}
}
