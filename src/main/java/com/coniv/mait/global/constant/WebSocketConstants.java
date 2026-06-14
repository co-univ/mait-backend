package com.coniv.mait.global.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WebSocketConstants {

	private static final String PARTICIPATE_TOPIC_PREFIX = "/topic/question-sets/";
	private static final String PARTICIPATE_TOPIC_SUFFIX = "/participate";

	public static String getQuestionSetParticipateTopic(Long questionSetId) {
		return PARTICIPATE_TOPIC_PREFIX + questionSetId + PARTICIPATE_TOPIC_SUFFIX;
	}

	public static Long parseParticipateQuestionSetId(String destination) {
		if (destination == null
			|| !destination.startsWith(PARTICIPATE_TOPIC_PREFIX)
			|| !destination.endsWith(PARTICIPATE_TOPIC_SUFFIX)) {
			return null;
		}
		String rawId = destination.substring(
			PARTICIPATE_TOPIC_PREFIX.length(),
			destination.length() - PARTICIPATE_TOPIC_SUFFIX.length());
		try {
			return Long.valueOf(rawId);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static String getQuestionSetManageTopic(Long questionSetId) {
		return "/topic/question-sets/" + questionSetId + "/manage";
	}

	public static String getQuestionSetParticipationStatusQueue(Long questionSetId) {
		return "/queue/question-sets/" + questionSetId + "/participation-status";
	}
}
