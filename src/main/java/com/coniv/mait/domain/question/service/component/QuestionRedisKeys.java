package com.coniv.mait.domain.question.service.component;

public final class QuestionRedisKeys {

	private static final String SUBMIT_ORDER_PREFIX = "$submit:order:questionId:";
	private static final String SUBMIT_FIRST_TIME_PREFIX = "$submit:first-time:questionId:";
	private static final String SCORER_PREFIX = "$scorer:questionId:";
	private static final String AI_STATUS_PREFIX = "question_set_ai_status:";
	private static final String REVIEW_PREFIX = "review:last_viewed:question_set:";
	private static final String USER_PREFIX = "user:";

	private static final String LIVE_PARTICIPANTS_PREFIX = "$ws:live:participants:question-set:";
	private static final String LIVE_USER_SESSIONS_PREFIX = "$ws:live:user-sessions:question-set:";
	private static final String LIVE_SESSION_PREFIX = "$ws:live:session:";

	private QuestionRedisKeys() {
	}

	public static String submitOrder(final Long questionId) {
		return SUBMIT_ORDER_PREFIX + questionId;
	}

	public static String submitFirstTime(final Long questionId) {
		return SUBMIT_FIRST_TIME_PREFIX + questionId;
	}

	public static String scorer(final Long questionId) {
		return SCORER_PREFIX + questionId;
	}

	public static String aiStatus(final Long questionSetId) {
		return AI_STATUS_PREFIX + questionSetId;
	}

	public static String reviewLastViewed(final Long questionSetId, final Long userId) {
		return REVIEW_PREFIX + questionSetId + USER_PREFIX + userId;
	}

	public static String reviewLastViewedPattern(final Long questionSetId) {
		return REVIEW_PREFIX + questionSetId + USER_PREFIX + "*";
	}

	public static String liveParticipants(final Long questionSetId) {
		return LIVE_PARTICIPANTS_PREFIX + questionSetId;
	}

	public static String liveUserSessions(final Long questionSetId, final Long userId) {
		return LIVE_USER_SESSIONS_PREFIX + questionSetId + ":" + USER_PREFIX + userId;
	}

	public static String liveSession(final String sessionId) {
		return LIVE_SESSION_PREFIX + sessionId;
	}
}
