package com.coniv.mait.domain.statistic.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.statistic.service.dto.QuestionAnswerDistributionDto;
import com.coniv.mait.domain.statistic.service.dto.SubmittedAnswerGroupDto;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionAnswerStatisticServiceTest {

	private static final Long QUESTION_SET_ID = 10L;
	private static final Long QUESTION_ID = 200L;
	private static final String ANSWER_1 = "{\"type\":\"MULTIPLE\",\"submitAnswers\":[1]}";
	private static final String ANSWER_1_2 = "{\"type\":\"MULTIPLE\",\"submitAnswers\":[1,2]}";

	@InjectMocks
	private QuestionAnswerStatisticService questionAnswerStatisticService;

	@Mock
	private QuestionService questionService;

	@Mock
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Test
	@DisplayName("getAnswerDistribution - firstSubmitOnly=false 면 전체 제출을 동일 답안끼리 그룹화하고, 정답률은 유저당 최초 제출 기준으로 계산한다")
	void getAnswerDistribution_allSubmits() {
		// given
		// user1: [1]오답(1) → [1,2]정답(2), user2: [1]오답(1), user3: [1]오답(1), user4: [1,2]정답(1)
		List<AnswerSubmitRecordEntity> records = List.of(
			record(1L, 1L, false, ANSWER_1),
			record(1L, 2L, true, ANSWER_1_2),
			record(2L, 1L, false, ANSWER_1),
			record(3L, 1L, false, ANSWER_1),
			record(4L, 1L, true, ANSWER_1_2));

		when(questionService.getQuestion(QUESTION_SET_ID, QUESTION_ID, DeliveryMode.MANAGING)).thenReturn(question());
		when(answerSubmitRecordEntityRepository.findAllByQuestionId(QUESTION_ID)).thenReturn(records);

		// when
		QuestionAnswerDistributionDto result =
			questionAnswerStatisticService.getAnswerDistribution(QUESTION_SET_ID, QUESTION_ID, false);

		// then
		assertThat(result.getQuestionSetId()).isEqualTo(QUESTION_SET_ID);
		assertThat(result.getTotalSubmitCount()).isEqualTo(5);
		assertThat(result.getSubmittedUserCount()).isEqualTo(4);
		assertThat(result.getCorrectUserCount()).isEqualTo(1);
		assertThat(result.getCorrectRate()).isEqualTo(25.0);

		// 제출 수 내림차순: [1](3건, 오답) > [1,2](2건, 정답)
		assertThat(result.getGroups()).hasSize(2);
		SubmittedAnswerGroupDto first = result.getGroups().get(0);
		assertThat(first.getSubmitCount()).isEqualTo(3);
		assertThat(first.isCorrect()).isFalse();
		assertThat(first.getSubmittedAnswer().getType()).isEqualTo(QuestionType.MULTIPLE);
		assertThat(first.getSubmittedAnswer().getSubmitAnswers()).isEqualTo(List.of(1L));

		SubmittedAnswerGroupDto second = result.getGroups().get(1);
		assertThat(second.getSubmitCount()).isEqualTo(2);
		assertThat(second.isCorrect()).isTrue();
		assertThat(second.getSubmittedAnswer().getSubmitAnswers()).isEqualTo(List.of(1L, 2L));
	}

	@Test
	@DisplayName("getAnswerDistribution - firstSubmitOnly=true 면 유저당 최초 제출만으로 분포를 구성하고 totalSubmitCount가 제출 유저 수와 같다")
	void getAnswerDistribution_firstSubmitOnly() {
		// given
		List<AnswerSubmitRecordEntity> records = List.of(
			record(1L, 1L, false, ANSWER_1),
			record(1L, 2L, true, ANSWER_1_2),
			record(2L, 1L, false, ANSWER_1),
			record(3L, 1L, false, ANSWER_1),
			record(4L, 1L, true, ANSWER_1_2));

		when(questionService.getQuestion(QUESTION_SET_ID, QUESTION_ID, DeliveryMode.MANAGING)).thenReturn(question());
		when(answerSubmitRecordEntityRepository.findAllByQuestionId(QUESTION_ID)).thenReturn(records);

		// when
		QuestionAnswerDistributionDto result =
			questionAnswerStatisticService.getAnswerDistribution(QUESTION_SET_ID, QUESTION_ID, true);

		// then
		// 최초 제출만: user1 [1]오답, user2 [1]오답, user3 [1]오답, user4 [1,2]정답
		assertThat(result.getTotalSubmitCount()).isEqualTo(4);
		assertThat(result.getTotalSubmitCount()).isEqualTo(result.getSubmittedUserCount());
		assertThat(result.getCorrectUserCount()).isEqualTo(1);
		assertThat(result.getCorrectRate()).isEqualTo(25.0);

		assertThat(result.getGroups()).hasSize(2);
		assertThat(result.getGroups().get(0).getSubmitCount()).isEqualTo(3);
		assertThat(result.getGroups().get(0).getSubmittedAnswer().getSubmitAnswers()).isEqualTo(List.of(1L));
		assertThat(result.getGroups().get(1).getSubmitCount()).isEqualTo(1);
		assertThat(result.getGroups().get(1).getSubmittedAnswer().getSubmitAnswers()).isEqualTo(List.of(1L, 2L));
	}

	@Test
	@DisplayName("getAnswerDistribution - 제출 기록이 없으면 그룹은 비어있고 카운트는 0, 정답률은 null이다")
	void getAnswerDistribution_noRecords() {
		// given
		when(questionService.getQuestion(QUESTION_SET_ID, QUESTION_ID, DeliveryMode.MANAGING)).thenReturn(question());
		when(answerSubmitRecordEntityRepository.findAllByQuestionId(QUESTION_ID)).thenReturn(List.of());

		// when
		QuestionAnswerDistributionDto result =
			questionAnswerStatisticService.getAnswerDistribution(QUESTION_SET_ID, QUESTION_ID, false);

		// then
		assertThat(result.getGroups()).isEmpty();
		assertThat(result.getTotalSubmitCount()).isZero();
		assertThat(result.getSubmittedUserCount()).isZero();
		assertThat(result.getCorrectUserCount()).isZero();
		assertThat(result.getCorrectRate()).isNull();
	}

	@Test
	@DisplayName("getAnswerDistribution - 문제를 찾지 못하면 예외가 전파되고 제출 기록은 조회하지 않는다")
	void getAnswerDistribution_questionNotFound() {
		// given
		when(questionService.getQuestion(QUESTION_SET_ID, QUESTION_ID, DeliveryMode.MANAGING))
			.thenThrow(new EntityNotFoundException("Question not found with id: " + QUESTION_ID));

		// when & then
		assertThatThrownBy(() ->
			questionAnswerStatisticService.getAnswerDistribution(QUESTION_SET_ID, QUESTION_ID, false))
			.isInstanceOf(EntityNotFoundException.class);

		verify(answerSubmitRecordEntityRepository, never()).findAllByQuestionId(any());
	}

	private QuestionDto question() {
		return MultipleQuestionDto.builder()
			.id(QUESTION_ID)
			.content("문제 내용")
			.explanation("해설")
			.choices(List.of())
			.build();
	}

	private AnswerSubmitRecordEntity record(final Long userId, final Long submitOrder, final boolean isCorrect,
		final String submittedAnswer) {
		return AnswerSubmitRecordEntity.builder()
			.userId(userId)
			.questionId(QUESTION_ID)
			.submitOrder(submitOrder)
			.isCorrect(isCorrect)
			.submittedAnswer(submittedAnswer)
			.build();
	}
}
