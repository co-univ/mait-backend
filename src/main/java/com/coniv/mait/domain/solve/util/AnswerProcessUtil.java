package com.coniv.mait.domain.solve.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnswerProcessUtil {

	public static String processAnswer(String answer) {
		if (answer == null || answer.isEmpty()) {
			return "";
		}
		return answer.trim().toLowerCase();
	}
}
