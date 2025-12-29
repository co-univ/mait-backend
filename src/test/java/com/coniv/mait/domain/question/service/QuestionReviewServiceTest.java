package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.LastViewedQuestionRedisRepository;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.dto.AnswerSubmitDto;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;

@ExtendWith(MockitoExtension.class)
class QuestionReviewServiceTest {

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private AnswerGrader answerGrader;

	@Mock
	private LastViewedQuestionRedisRepository lastViewedQuestionRedisRepository;

	@Mock
	private QuestionFactory<?> shortQuestionFactory;

	@Mock
	private QuestionFactory<?> multipleQuestionFactory;

	@Mock
	private QuestionFactory<?> orderingQuestionFactory;

	@Mock
	private QuestionFactory<?> fillBlankQuestionFactory;

	private QuestionReviewService questionReviewService;

	@BeforeEach
	void setUp() {
		when(shortQuestionFactory.getQuestionType()).thenReturn(QuestionType.SHORT);
		when(multipleQuestionFactory.getQuestionType()).thenReturn(QuestionType.MULTIPLE);
		when(orderingQuestionFactory.getQuestionType()).thenReturn(QuestionType.ORDERING);
		when(fillBlankQuestionFactory.getQuestionType()).thenReturn(QuestionType.FILL_BLANK);

		questionReviewService = new QuestionReviewService(
			questionSetEntityRepository,
			teamRoleValidator,
			lastViewedQuestionRedisRepository,
			answerGrader,
			questionReader,
			List.of(shortQuestionFactory, multipleQuestionFactory, orderingQuestionFactory,
				fillBlankQuestionFactory));
	}

	@Test
	@DisplayName("복습에서 마지막으로 본 문제 조회 성공")
	void getLastViewedQuestionInReview_success() {
		// given
		final Long questionSetId = 1L;
		final Long userId = 2L;
		final Long teamId = 3L;
		final boolean answerVisible = DeliveryMode.REVIEW.isAnswerVisible();
		final QuestionType questionType = QuestionType.MULTIPLE;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getOngoingStatus()).thenReturn(QuestionSetOngoingStatus.AFTER);
		when(questionSet.getTeamId()).thenReturn(teamId);

		QuestionEntity lastViewedQuestion = mock(QuestionEntity.class);
		when(lastViewedQuestion.getType()).thenReturn(questionType);

		QuestionDto expected = mock(QuestionDto.class);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(lastViewedQuestionRedisRepository.getLastViewedQuestion(questionSet, userId)).thenReturn(
			lastViewedQuestion);
		when(multipleQuestionFactory.getQuestion(lastViewedQuestion, answerVisible)).thenReturn(expected);

		// when
		QuestionDto result = questionReviewService.getLastViewedQuestionInReview(questionSetId, userId);

		// then
		assertThat(result).isSameAs(expected);
		verify(questionSetEntityRepository).findById(questionSetId);
		verify(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);
		verify(lastViewedQuestionRedisRepository).getLastViewedQuestion(questionSet, userId);
		verify(multipleQuestionFactory).getQuestion(lastViewedQuestion, answerVisible);
		verify(shortQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(orderingQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(fillBlankQuestionFactory, never()).getQuestion(any(), anyBoolean());
		verify(questionReader, never()).getQuestion(anyLong());
		verify(questionReader, never()).getQuestion(anyLong(), anyLong());
	}

	@Test
	@DisplayName("복습 시 마지막 문제 조회 실패 - 문제 셋이 종료 상태가 아님")
	void getLastViewedQuestionInReview_notAfter() {
		// given
		final Long questionSetId = 1L;
		final Long userId = 2L;

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getOngoingStatus()).thenReturn(QuestionSetOngoingStatus.ONGOING);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));

		// when, then
		QuestionSetStatusException questionSetStatusException = assertThrows(QuestionSetStatusException.class,
			() -> questionReviewService.getLastViewedQuestionInReview(questionSetId, userId));

		assertThat(questionSetStatusException.getExceptionCode()).isEqualTo(QuestionSetStatusExceptionCode.ONLY_AFTER);
		verify(teamRoleValidator, never()).checkHasSolveQuestionAuthorityInTeam(anyLong(), anyLong());
		verify(lastViewedQuestionRedisRepository, never()).getLastViewedQuestion(any(QuestionSetEntity.class),
			anyLong());
	}

	@Test
	@DisplayName("복습 문제 풀이 - 성공")
	void checkAnswer() {
		// given
		final Long questionId = 1L;
		final Long userId = 2L;
		final Long questionSetId = 3L;
		MultipleQuestionSubmitAnswer multipleQuestionSubmitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));
		QuestionEntity question = mock(QuestionEntity.class);
		when(questionReader.getQuestion(questionId)).thenReturn(question);

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(mock(QuestionSetEntity.class)));

		when(questionSet.canReview()).thenReturn(true);
		when(answerGrader.gradeAnswer(question, multipleQuestionSubmitAnswer)).thenReturn(true);

		// when
		AnswerSubmitDto answerSubmitDto = questionReviewService.checkAnswer(questionId, questionSetId, userId,
			multipleQuestionSubmitAnswer);

		// then
		assertThat(answerSubmitDto.getQuestionId()).isEqualTo(questionId);
		assertThat(answerSubmitDto.isCorrect()).isEqualTo(true);
	}

	@Test
	@DisplayName("복습 문제 풀이 - 문제 셋이 리뷰상태가 아니면 실패")
	void checkAnswer_cannotReview() {
		// given
		final Long questionId = 1L;
		final Long userId = 2L;
		final Long questionSetId = 3L;
		MultipleQuestionSubmitAnswer multipleQuestionSubmitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));
		QuestionEntity question = mock(QuestionEntity.class);
		when(questionReader.getQuestion(questionId)).thenReturn(question);

		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));

		when(questionSet.canReview()).thenReturn(false);

		// when
		QuestionSetStatusException questionSetStatusException = assertThrows(QuestionSetStatusException.class,
			() -> questionReviewService.checkAnswer(questionId, questionSetId, userId, multipleQuestionSubmitAnswer));

		// then
		assertThat(questionSetStatusException.getExceptionCode()).isEqualTo(QuestionSetStatusExceptionCode.ONLY_REVIEW);
	}
}
