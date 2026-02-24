package com.coniv.mait.domain.solve.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SolvingStatus {
	/**
	 * 문제 풀이 중
	 */
	PROGRESSING,
	/**
	 *문제 풀이 완료
	 */
	COMPLETE;
}
