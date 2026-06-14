package com.coniv.mait.global.constant;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebSocketConstantsTest {

	@Test
	@DisplayName("participate 토픽 destination 에서 questionSetId 를 파싱한다")
	void parseParticipateQuestionSetId_validDestination() {
		Long result = WebSocketConstants.parseParticipateQuestionSetId("/topic/question-sets/7/participate");

		assertThat(result).isEqualTo(7L);
	}

	@Test
	@DisplayName("participate 토픽이 아니면 null 을 반환한다")
	void parseParticipateQuestionSetId_otherTopic_returnsNull() {
		Long result = WebSocketConstants.parseParticipateQuestionSetId("/topic/question-sets/7/manage");

		assertThat(result).isNull();
	}

	@Test
	@DisplayName("destination 이 null 이면 null 을 반환한다")
	void parseParticipateQuestionSetId_null_returnsNull() {
		Long result = WebSocketConstants.parseParticipateQuestionSetId(null);

		assertThat(result).isNull();
	}

	@Test
	@DisplayName("questionSetId 가 숫자가 아니면 null 을 반환한다")
	void parseParticipateQuestionSetId_nonNumericId_returnsNull() {
		Long result = WebSocketConstants.parseParticipateQuestionSetId("/topic/question-sets/abc/participate");

		assertThat(result).isNull();
	}
}
