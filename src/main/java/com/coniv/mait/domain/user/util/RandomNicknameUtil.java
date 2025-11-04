package com.coniv.mait.domain.user.util;

import java.util.List;

import com.coniv.mait.global.util.RandomUtil;

public class RandomNicknameUtil {

	private RandomNicknameUtil() {
	}

	private static final List<String> ADJECTIVES = List.of(
		"빠른", "느린", "차가운", "따뜻한", "밝은",
		"어두운", "귀여운", "멋진", "작은", "큰",
		"젊은", "오래된", "새로운", "고요한", "시끄러운",
		"신비한", "매혹적인", "순수한", "거친", "부드러운",
		"강한", "약한", "푸른", "붉은", "검은",
		"흰", "황금빛", "은빛", "무지개빛", "반짝이는",
		"흐린", "화려한", "단단한", "가벼운", "무거운",
		"깊은", "얕은", "용감한", "겁많은", "즐거운",
		"슬픈", "행복한", "외로운", "자유로운", "날카로운",
		"둥근", "긴", "짧은", "달콤한", "씁쓸한"
	);

	private static final List<String> NOUNS = List.of(
		"고양이", "강아지", "호랑이", "사자", "여우",
		"늑대", "곰", "토끼", "판다", "코끼리",
		"돌고래", "상어", "펭귄", "부엉이", "독수리",
		"까마귀", "고래", "거북이", "뱀", "용",
		"별", "달", "태양", "구름", "비",
		"바람", "불꽃", "바다", "강", "산",
		"숲", "나무", "꽃", "풀", "돌",
		"모래", "별빛", "불빛", "그림자", "파도",
		"하늘", "무지개", "별똥별", "은하", "시냇물",
		"얼음", "눈", "불", "새벽", "저녁"
	);

	public static String generateRandomNickname() {
		String adjective = ADJECTIVES.get(RandomUtil.getRandomNumber(ADJECTIVES.size()));
		String noun = NOUNS.get(RandomUtil.getRandomNumber(NOUNS.size()));
		return adjective + " " + noun;
	}
}

