package com.coniv.mait.util;

import java.security.SecureRandom;

/**
 * 임시 비밀번호 생성 유틸리티 클래스
 */
public class TemporaryPasswordGenerator {

	private static final String[] SPECIAL_CHARS = {"!", "@", "#"};
	private static final SecureRandom RANDOM = new SecureRandom();

	/**
	 * temp + 3자리 숫자 + 특수문자 형태의 임시 비밀번호 생성
	 * 예: temp123!, temp456@, temp789#
	 *
	 * @return 생성된 임시 비밀번호
	 */
	public static String generateTemporaryPassword() {
		// 3자리 랜덤 숫자 생성 (100-999)
		int randomNumber = RANDOM.nextInt(900) + 100;

		// 특수문자 랜덤 선택
		String specialChar = SPECIAL_CHARS[RANDOM.nextInt(SPECIAL_CHARS.length)];

		return "temp" + randomNumber + specialChar;
	}
}
