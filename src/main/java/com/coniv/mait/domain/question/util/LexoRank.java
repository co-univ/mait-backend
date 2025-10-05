package com.coniv.mait.domain.question.util;

import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LexoRank {

	private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final int BASE = ALPHABET.length();
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
				return prefix.append(ALPHABET.charAt(mid)).toString();
			}

			prefix.append(ALPHABET.charAt(prevIndex));
			index++;
		}
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
