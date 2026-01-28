package com.coniv.mait.global.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WebSocketConstants {

	// Topic Patterns (Regex)
	public static final String QUESTION_SET_PARTICIPATE_TOPIC_PATTERN = "^/topic/question-sets/(\\d+)/participate$";

	// Topic Builders
	public static String getQuestionSetParticipateTopic(Long questionSetId) {
		return "/topic/question-sets/" + questionSetId + "/participate";
	}
}
