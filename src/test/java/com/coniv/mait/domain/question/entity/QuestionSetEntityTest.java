package com.coniv.mait.domain.question.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;

class QuestionSetEntityTest {

	@ParameterizedTest
	@CsvSource({"LIVE_TIME, LIVE_TIME, ONGOING", "REVIEW, LIVE_TIME, REVIEW", "REVIEW, STUDY, REVIEW",
		"STUDY, STUDY, ONGOING", "STUDY, STUDY, AFTER", "MAKING, STUDY, MAKING",
		"MANAGING, LIVE_TIME, BEFORE", "MANAGING, LIVE_TIME, AFTER"})
	@DisplayName("문제셋 상태에 맞는 풀이 조회와 상태 제한이 없는 편집 및 관리 조회를 허용한다")
	void validateDeliveryMode_permitted(DeliveryMode mode, QuestionSetSolveMode solveMode,
		QuestionSetStatus status) {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder().solveMode(solveMode).status(status).build();

		// when & then
		assertThatCode(() -> questionSet.validateDeliveryMode(mode)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@CsvSource({"LIVE_TIME, LIVE_TIME, BEFORE, ONLY_LIVE_TIME",
		"LIVE_TIME, LIVE_TIME, AFTER, ONLY_LIVE_TIME", "LIVE_TIME, LIVE_TIME, REVIEW, ONLY_LIVE_TIME",
		"LIVE_TIME, STUDY, ONGOING, ONLY_LIVE_TIME", "REVIEW, LIVE_TIME, BEFORE, ONLY_REVIEW",
		"REVIEW, LIVE_TIME, ONGOING, ONLY_REVIEW", "REVIEW, LIVE_TIME, AFTER, ONLY_REVIEW",
		"STUDY, LIVE_TIME, ONGOING, ONLY_STUDY"})
	@DisplayName("문제셋 상태와 맞지 않는 풀이 모드는 기존 오류 코드로 거절한다")
	void validateDeliveryMode_invalidMode(DeliveryMode mode, QuestionSetSolveMode solveMode,
		QuestionSetStatus status, QuestionSetStatusExceptionCode code) {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder().solveMode(solveMode).status(status).build();

		// when & then
		assertThatThrownBy(() -> questionSet.validateDeliveryMode(mode))
			.isInstanceOfSatisfying(QuestionSetStatusException.class,
				exception -> assertThat(exception.getExceptionCode()).isEqualTo(code));
	}
}
