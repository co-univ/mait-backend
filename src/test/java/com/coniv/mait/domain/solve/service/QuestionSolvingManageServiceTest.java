package com.coniv.mait.domain.solve.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionExceptionCode;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.QuestionStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.domain.solve.component.QuestionAnswerUpdater;
import com.coniv.mait.domain.solve.event.QuestionUpdateEvent;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.global.event.MaitEventPublisher;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.web.solve.dto.FillBlankUpdateAnswerPayload;
import com.coniv.mait.web.solve.dto.MultipleChoiceUpdateAnswerPayload;
import com.coniv.mait.web.solve.dto.ShortUpdateAnswerPayload;
import com.coniv.mait.web.solve.dto.UpdateAnswerPayload;

@ExtendWith(MockitoExtension.class)
class QuestionSolvingManageServiceTest {

	private QuestionSolvingManageService questionSolvingManageService;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private MaitEventPublisher maitEventPublisher;

	@Mock
	private QuestionAnswerUpdater shortQuestionAnswerUpdater;

	@Mock
	private QuestionAnswerUpdater fillBlankQuestionAnswerUpdater;

	@BeforeEach
	void setUp() {
		when(shortQuestionAnswerUpdater.getQuestionType()).thenReturn(QuestionType.SHORT);
		when(fillBlankQuestionAnswerUpdater.getQuestionType()).thenReturn(QuestionType.FILL_BLANK);

		questionSolvingManageService = new QuestionSolvingManageService(
			List.of(shortQuestionAnswerUpdater, fillBlankQuestionAnswerUpdater),
			questionReader,
			maitEventPublisher
		);
	}

	@Test
	@DisplayName("문제 답안 수정 - 성공 (SHORT)")
	void updateQuestionAnswers_success_short() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 2L;
		ShortUpdateAnswerPayload request = new ShortUpdateAnswerPayload(List.of(
			ShortAnswerDto.builder().id(10L).answer("answer").main(false).number(1L).build()
		));

		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.isOnLive()).thenReturn(true);
		when(question.getType()).thenReturn(QuestionType.SHORT);

		// when
		questionSolvingManageService.updateQuestionAnswers(questionSetId, questionId, request);

		// then
		verify(questionReader).getQuestion(questionId, questionSetId);
		verify(shortQuestionAnswerUpdater).updateAnswer(question, request);
		verify(fillBlankQuestionAnswerUpdater, never()).updateAnswer(any(), any());

		ArgumentCaptor<QuestionUpdateEvent> captor = ArgumentCaptor.forClass(QuestionUpdateEvent.class);
		verify(maitEventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().questionId()).isEqualTo(questionId);
	}

	@Test
	@DisplayName("문제 답안 수정 - 성공 (FILL_BLANK)")
	void updateQuestionAnswers_success_fillBlank() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 2L;
		FillBlankUpdateAnswerPayload request = new FillBlankUpdateAnswerPayload(List.of(
			FillBlankAnswerDto.builder().id(10L).answer("answer").main(false).number(1L).build()
		));

		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.isOnLive()).thenReturn(true);
		when(question.getType()).thenReturn(QuestionType.FILL_BLANK);

		// when
		questionSolvingManageService.updateQuestionAnswers(questionSetId, questionId, request);

		// then
		verify(questionReader).getQuestion(questionId, questionSetId);
		verify(fillBlankQuestionAnswerUpdater).updateAnswer(question, request);
		verify(shortQuestionAnswerUpdater, never()).updateAnswer(any(), any());

		ArgumentCaptor<QuestionUpdateEvent> captor = ArgumentCaptor.forClass(QuestionUpdateEvent.class);
		verify(maitEventPublisher).publishEvent(captor.capture());
		assertThat(captor.getValue().questionId()).isEqualTo(questionId);
	}

	@Test
	@DisplayName("문제 답안 수정 - 실패 (실시간 상태가 아니면 ONLY_LIVE_TIME)")
	void updateQuestionAnswers_fail_onlyLiveTime() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 2L;
		ShortUpdateAnswerPayload request = new ShortUpdateAnswerPayload(List.of(
			ShortAnswerDto.builder().id(10L).answer("answer").main(true).number(1L).build()
		));

		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.isOnLive()).thenReturn(false);

		// when, then
		QuestionSetStatusException exception = assertThrows(QuestionSetStatusException.class,
			() -> questionSolvingManageService.updateQuestionAnswers(questionSetId, questionId, request));
		assertThat(exception.getExceptionCode()).isEqualTo(QuestionSetStatusExceptionCode.ONLY_LIVE_TIME);

		verify(shortQuestionAnswerUpdater, never()).updateAnswer(any(), any());
		verify(fillBlankQuestionAnswerUpdater, never()).updateAnswer(any(), any());
		verify(maitEventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("문제 답안 수정 - 실패 (SHORT/FILL_BLANK 외 타입이면 UNAVAILABLE_TYPE)")
	void updateQuestionAnswers_fail_unavailableType() {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 2L;
		MultipleChoiceUpdateAnswerPayload request = new MultipleChoiceUpdateAnswerPayload(Set.of(1L));

		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.isOnLive()).thenReturn(true);

		// when, then
		QuestionStatusException exception = assertThrows(QuestionStatusException.class,
			() -> questionSolvingManageService.updateQuestionAnswers(questionSetId, questionId, request));
		assertThat(exception.getQuestionExceptionCode()).isEqualTo(QuestionExceptionCode.UNAVAILABLE_TYPE);

		verify(shortQuestionAnswerUpdater, never()).updateAnswer(any(), any());
		verify(fillBlankQuestionAnswerUpdater, never()).updateAnswer(any(), any());
		verify(maitEventPublisher, never()).publishEvent(any());
	}

	@ParameterizedTest
	@CsvSource({
		"SHORT, FILL_BLANK", "FILL_BLANK, SHORT",
		"MULTIPLE, SHORT", "MULTIPLE, FILL_BLANK",
		"ORDERING, SHORT", "ORDERING, FILL_BLANK"
	})
	@DisplayName("실제 문제와 요청 유형이 다르면 답안 추가와 재채점 이벤트를 차단한다")
	void updateQuestionAnswers_rejectsMismatchedType(QuestionType questionType, QuestionType requestType) {
		// given
		UpdateAnswerPayload request;
		if (requestType == QuestionType.SHORT) {
			request = new ShortUpdateAnswerPayload(List.of(
				ShortAnswerDto.builder().number(1L).answer("answer").main(false).build()));
		} else {
			request = new FillBlankUpdateAnswerPayload(List.of(
				FillBlankAnswerDto.builder().number(1L).answer("answer").main(false).build()));
		}
		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionReader.getQuestion(2L, 1L)).thenReturn(question);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.isOnLive()).thenReturn(true);
		when(question.getType()).thenReturn(questionType);

		// when & then
		assertThatThrownBy(() -> questionSolvingManageService.updateQuestionAnswers(1L, 2L, request))
			.isInstanceOfSatisfying(QuestionSolvingException.class, exception ->
				assertThat(exception.getExceptionCode()).isEqualTo(QuestionSolveExceptionCode.INVALID_TYPE));
		verify(shortQuestionAnswerUpdater, never()).updateAnswer(any(), any());
		verify(fillBlankQuestionAnswerUpdater, never()).updateAnswer(any(), any());
		verify(maitEventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("인정답안 검증에 실패하면 재채점 이벤트를 발행하지 않는다")
	void updateQuestionAnswers_doesNotPublishWhenValidationFails() {
		// given
		ShortUpdateAnswerPayload request = new ShortUpdateAnswerPayload(List.of(
			ShortAnswerDto.builder().number(3L).answer("answer").main(false).build()));
		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionReader.getQuestion(2L, 1L)).thenReturn(question);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.isOnLive()).thenReturn(true);
		when(question.getType()).thenReturn(QuestionType.SHORT);
		UserParameterException exception = new UserParameterException("해당 문제에 존재하지 않는 정답 그룹입니다.");
		doThrow(exception).when(shortQuestionAnswerUpdater).updateAnswer(question, request);

		// when & then
		assertThatThrownBy(() -> questionSolvingManageService.updateQuestionAnswers(1L, 2L, request))
			.isSameAs(exception);
		verify(maitEventPublisher, never()).publishEvent(any());
	}
}
