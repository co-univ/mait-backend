package com.coniv.mait.domain.statistic.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.service.component.AnswerSubmitRecordReader;
import com.coniv.mait.domain.statistic.service.dto.QuestionStatisticDto;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionStatisticServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long QUESTION_SET_ID = 10L;
	private static final Long TEAM_ID = 100L;
	private static final MaitUser MAIT_USER = MaitUser.builder().id(USER_ID).build();

	@InjectMocks
	private QuestionStatisticService questionStatisticService;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private QuestionSetReader questionSetReader;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private AnswerSubmitRecordReader answerSubmitRecordReader;

	@Test
	@DisplayName("getWrongRates - 문제별 오답률을 계산해 오답률이 높은 순으로 정렬하고, 제출자가 없는 문제는 null로 맨 뒤에 둔다")
	void getWrongRates_success() {
		// given
		QuestionSetEntity questionSet = mockQuestionSet();
		QuestionEntity q1 = mockQuestion(101L, 1L);
		QuestionEntity q2 = mockQuestion(102L, 2L);
		QuestionEntity q3 = mockQuestion(103L, 3L);

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);
		when(questionReader.getQuestionsByQuestionSet(questionSet)).thenReturn(List.of(q1, q2, q3));
		when(answerSubmitRecordReader.getFirstSubmitsByQuestionId(anyList())).thenReturn(Map.of(
			// q1: 4명 제출, 최초 오답 1명 → 25.0
			101L, List.of(firstSubmit(false), firstSubmit(true), firstSubmit(true), firstSubmit(true)),
			// q2: 2명 제출, 최초 오답 2명 → 100.0
			102L, List.of(firstSubmit(false), firstSubmit(false))));
		// q3(103L): 제출 없음 → null

		// when
		List<QuestionStatisticDto> result = questionStatisticService.getWrongRates(QUESTION_SET_ID, MAIT_USER);

		// then
		assertThat(result).hasSize(3);
		// 오답률 내림차순: q2(100.0) > q1(25.0) > q3(null)
		assertThat(result.get(0).getQuestionId()).isEqualTo(102L);
		assertThat(result.get(0).getSubmittedUserCount()).isEqualTo(2);
		assertThat(result.get(0).getFirstWrongUserCount()).isEqualTo(2);
		assertThat(result.get(0).getWrongRate()).isEqualTo(100.0);
		assertThat(result.get(1).getQuestionId()).isEqualTo(101L);
		assertThat(result.get(1).getSubmittedUserCount()).isEqualTo(4);
		assertThat(result.get(1).getFirstWrongUserCount()).isEqualTo(1);
		assertThat(result.get(1).getWrongRate()).isEqualTo(25.0);
		assertThat(result.get(2).getQuestionId()).isEqualTo(103L);
		assertThat(result.get(2).getSubmittedUserCount()).isEqualTo(0);
		assertThat(result.get(2).getWrongRate()).isNull();
	}

	@Test
	@DisplayName("getWrongRates - 문제 셋에 문제가 없으면 빈 목록을 반환하고 제출 기록을 조회하지 않는다")
	void getWrongRates_emptyQuestions_returnsEmpty() {
		// given
		QuestionSetEntity questionSet = mockQuestionSet();
		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);
		when(questionReader.getQuestionsByQuestionSet(questionSet)).thenReturn(List.of());

		// when
		List<QuestionStatisticDto> result = questionStatisticService.getWrongRates(QUESTION_SET_ID, MAIT_USER);

		// then
		assertThat(result).isEmpty();
		verify(answerSubmitRecordReader, never()).getFirstSubmitsByQuestionId(any());
	}

	@Test
	@DisplayName("getWrongRates - 팀 멤버가 아니면 UserRoleException이 발생한다")
	void getWrongRates_notTeamMember_throwsUserRoleException() {
		// given
		QuestionSetEntity questionSet = mockQuestionSet();
		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);
		doThrow(new UserRoleException("해당 팀의 멤버가 아닙니다."))
			.when(teamRoleValidator).checkIsTeamMember(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> questionStatisticService.getWrongRates(QUESTION_SET_ID, MAIT_USER))
			.isInstanceOf(UserRoleException.class)
			.hasMessage("해당 팀의 멤버가 아닙니다.");
	}

	@Test
	@DisplayName("getWrongRates - 존재하지 않는 문제 셋이면 EntityNotFoundException이 발생한다")
	void getWrongRates_questionSetNotFound_throwsEntityNotFoundException() {
		// given
		when(questionSetReader.getQuestionSet(QUESTION_SET_ID))
			.thenThrow(new EntityNotFoundException("해당 문제 셋을 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() -> questionStatisticService.getWrongRates(QUESTION_SET_ID, MAIT_USER))
			.isInstanceOf(EntityNotFoundException.class);
	}

	private QuestionSetEntity mockQuestionSet() {
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		lenient().when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		return questionSet;
	}

	private QuestionEntity mockQuestion(final Long id, final Long number) {
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(id);
		when(question.getNumber()).thenReturn(number);
		return question;
	}

	private AnswerSubmitRecordEntity firstSubmit(final boolean isCorrect) {
		return AnswerSubmitRecordEntity.builder()
			.userId(USER_ID)
			.questionId(101L)
			.isCorrect(isCorrect)
			.submitOrder(1L)
			.build();
	}
}
