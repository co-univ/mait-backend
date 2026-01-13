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
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.LastViewedQuestionRedisRepository;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.ReviewAnswerGrader;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

@ExtendWith(MockitoExtension.class)
class QuestionReviewServiceTest {

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private ReviewAnswerGrader reviewAnswerGrader;

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
			reviewAnswerGrader,
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
	void checkReviewAnswer_success() {
		// given
		final Long questionId = 1L;
		final Long userId = 2L;
		final Long questionSetId = 3L;
		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);

		ReviewAnswerCheckResult expectedResult = ReviewAnswerCheckResult.builder()
			.questionId(questionId)
			.isCorrect(true)
			.type(QuestionType.MULTIPLE)
			.gradedResults(List.of())
			.build();

		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSet.getVisibility()).thenReturn(QuestionSetVisibility.PUBLIC);
		when(questionSet.canReview()).thenReturn(true);
		when(reviewAnswerGrader.gradeAnswer(questionId, question, submitAnswer)).thenReturn(expectedResult);

		// when
		ReviewAnswerCheckResult result = questionReviewService.checkReviewAnswer(questionId, questionSetId, userId,
			submitAnswer);

		// then
		assertThat(result.questionId()).isEqualTo(questionId);
		assertThat(result.isCorrect()).isTrue();
		verify(teamRoleValidator, never()).checkHasSolveQuestionAuthorityInTeam(anyLong(), anyLong());
		verify(questionSetEntityRepository).findById(questionSetId);
		verify(reviewAnswerGrader).gradeAnswer(questionId, question, submitAnswer);
	}

	@Test
	@DisplayName("복습 문제 풀이 - 문제 셋이 리뷰상태가 아니면 실패")
	void checkReviewAnswer_cannotReview() {
		// given
		final Long questionId = 1L;
		final Long userId = 2L;
		final Long questionSetId = 3L;
		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSet.getVisibility()).thenReturn(QuestionSetVisibility.PUBLIC);

		when(questionSet.canReview()).thenReturn(false);

		// when
		QuestionSetStatusException exception = assertThrows(QuestionSetStatusException.class,
			() -> questionReviewService.checkReviewAnswer(questionId, questionSetId, userId, submitAnswer));

		// then
		assertThat(exception.getExceptionCode()).isEqualTo(QuestionSetStatusExceptionCode.ONLY_REVIEW);
		verify(questionSetEntityRepository).findById(questionSetId);
		verify(reviewAnswerGrader, never()).gradeAnswer(anyLong(), any(), any());
	}

	@Test
	@DisplayName("복습 문제 풀이 - 해당 문제가 문제 셋에 속하지 않으면 실패")
	void checkReviewAnswer_questionNotBelongToQuestionSet() {
		// given
		final Long questionId = 1L;
		final Long userId = 2L;
		final Long questionSetId = 3L;
		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

		when(questionReader.getQuestion(questionId, questionSetId))
			.thenThrow(new ResourceNotBelongException("문제가 해당 문제 세트에 속하지 않습니다."));

		// when, then
		assertThatThrownBy(() -> questionReviewService.checkReviewAnswer(questionId, questionSetId, userId,
			submitAnswer))
			.isInstanceOf(ResourceNotBelongException.class);

		verify(questionSetEntityRepository, never()).findById(anyLong());
		verify(teamRoleValidator, never()).checkHasSolveQuestionAuthorityInTeam(anyLong(), anyLong());
		verify(reviewAnswerGrader, never()).gradeAnswer(anyLong(), any(), any());
	}

	@Test
	@DisplayName("복습 문제 풀이 - 문제 셋이 PRIVATE면 NEED_OPEN 예외")
	void checkReviewAnswer_private_needOpen() {
		// given
		final Long questionId = 1L;
		final Long userId = 2L;
		final Long questionSetId = 3L;
		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);

		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSet.getVisibility()).thenReturn(QuestionSetVisibility.PRIVATE);

		// when, then
		QuestionSetStatusException exception = assertThrows(QuestionSetStatusException.class,
			() -> questionReviewService.checkReviewAnswer(questionId, questionSetId, userId, submitAnswer));

		assertThat(exception.getExceptionCode()).isEqualTo(QuestionSetStatusExceptionCode.NEED_OPEN);
		verify(teamRoleValidator, never()).checkHasSolveQuestionAuthorityInTeam(anyLong(), anyLong());
		verify(reviewAnswerGrader, never()).gradeAnswer(anyLong(), any(), any());
		verify(questionSetEntityRepository).findById(questionSetId);
	}

	@Test
	@DisplayName("복습 문제 풀이 - 문제 셋이 GROUP이면 팀 권한 검증 수행")
	void checkReviewAnswer_group_checkAuthority() {
		// given
		final Long questionId = 1L;
		final Long userId = 2L;
		final Long questionSetId = 3L;
		final Long teamId = 4L;
		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

		QuestionEntity question = mock(QuestionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);

		ReviewAnswerCheckResult expectedResult = ReviewAnswerCheckResult.builder()
			.questionId(questionId)
			.isCorrect(true)
			.type(QuestionType.MULTIPLE)
			.gradedResults(List.of())
			.build();

		when(questionReader.getQuestion(questionId, questionSetId)).thenReturn(question);
		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSet.getVisibility()).thenReturn(QuestionSetVisibility.GROUP);
		when(questionSet.getTeamId()).thenReturn(teamId);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);
		when(questionSet.canReview()).thenReturn(true);
		when(reviewAnswerGrader.gradeAnswer(questionId, question, submitAnswer)).thenReturn(expectedResult);

		// when
		ReviewAnswerCheckResult result = questionReviewService.checkReviewAnswer(questionId, questionSetId, userId,
			submitAnswer);

		// then
		assertThat(result.questionId()).isEqualTo(questionId);
		assertThat(result.isCorrect()).isTrue();
		verify(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);
		verify(questionSetEntityRepository).findById(questionSetId);
		verify(reviewAnswerGrader).gradeAnswer(questionId, question, submitAnswer);
	}
}
