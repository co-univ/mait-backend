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
import com.coniv.mait.global.event.MaitEventPublisher;
import com.coniv.mait.web.solve.dto.FillBlankUpdateAnswerPayload;
import com.coniv.mait.web.solve.dto.MultipleChoiceUpdateAnswerPayload;
import com.coniv.mait.web.solve.dto.ShortUpdateAnswerPayload;

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
			ShortAnswerDto.builder().id(10L).answer("answer").main(true).number(1L).build()
		));

		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.isOnLive()).thenReturn(true);

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
			FillBlankAnswerDto.builder().id(10L).answer("answer").main(true).number(1L).build()
		));

		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		when(question.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.isOnLive()).thenReturn(true);

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
}
