package com.coniv.mait.domain.statistic.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashMap;
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
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.component.AnswerSubmitRecordReader;
import com.coniv.mait.domain.solve.service.component.QuestionParticipantReader;
import com.coniv.mait.domain.statistic.service.SolvingResultService;
import com.coniv.mait.domain.statistic.service.component.QuestionSetStatisticCalculator;
import com.coniv.mait.domain.statistic.service.dto.MySolveRecordDto;
import com.coniv.mait.domain.statistic.service.dto.QuestionSetStatisticDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class SolvingResultServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long QUESTION_SET_ID = 10L;
	private static final Long TEAM_ID = 100L;
	private static final MaitUser MAIT_USER = MaitUser.builder().id(USER_ID).build();

	@InjectMocks
	private SolvingResultService solvingResultService;

	@Mock
	private QuestionSetReader questionSetReader;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private AnswerSubmitRecordReader answerSubmitRecordReader;

	@Mock
	private QuestionParticipantReader questionParticipantReader;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private QuestionSetStatisticCalculator questionSetStatisticCalculator;

	@Test
	@DisplayName("getMySolveRecord - 문제당 1건 기록(학습모드 형태)이면 전 문제가 결과에 포함되고 점수를 계산한다")
	void getSolvingResultsPerQuestion() {
		// given
		QuestionSetEntity questionSet = mockQuestionSet(QuestionSetSolveMode.STUDY);
		QuestionEntity question1 = mockQuestion(101L);
		QuestionEntity question2 = mockQuestion(102L);

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);
		when(questionReader.getOrderedQuestions(QUESTION_SET_ID)).thenReturn(List.of(question1, question2));
		when(answerSubmitRecordReader.getEarliestByQuestionId(USER_ID, List.of(101L, 102L)))
			.thenReturn(Map.of(
				101L, record(101L, true, null),
				102L, record(102L, false, null)));

		// when
		MySolveRecordDto result = solvingResultService.getSolvingResults(MAIT_USER, QUESTION_SET_ID);

		// then
		assertThat(result.getSolveMode()).isEqualTo(QuestionSetSolveMode.STUDY);
		assertThat(result.getTotalCount()).isEqualTo(2);
		assertThat(result.getCorrectCount()).isEqualTo(1);
		assertThat(result.getScore()).isEqualTo(50.0);
		assertThat(result.getResults()).hasSize(2);
		assertThat(result.getResults().get(0).getQuestionId()).isEqualTo(101L);
		assertThat(result.getResults().get(0).isCorrect()).isTrue();
		assertThat(result.getResults().get(1).getQuestionId()).isEqualTo(102L);
		assertThat(result.getResults().get(1).isCorrect()).isFalse();
	}

	@Test
	@DisplayName("getMySolveRecord - 문제당 여러 기록이면 가장 먼저 제출한 기록을 대표로 쓰고 안 푼 문제도 분모에 포함한다")
	void getSolvingResults_multipleRecordsPerQuestion() {
		// given
		QuestionSetEntity questionSet = mockQuestionSet(QuestionSetSolveMode.LIVE_TIME);
		QuestionEntity question1 = mockQuestion(101L);
		QuestionEntity question2 = mockQuestion(102L);
		QuestionEntity question3 = mockQuestion(103L);

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);
		when(questionReader.getOrderedQuestions(QUESTION_SET_ID))
			.thenReturn(List.of(question1, question2, question3));
		// reader가 최초 제출 기록을 선별해 반환 (101은 오답이 최초, 102는 정답 1건, 103은 미응답)
		when(answerSubmitRecordReader.getEarliestByQuestionId(USER_ID, List.of(101L, 102L, 103L)))
			.thenReturn(Map.of(
				101L, record(101L, false, 1L),
				102L, record(102L, true, 3L)));

		// when
		MySolveRecordDto result = solvingResultService.getSolvingResults(MAIT_USER, QUESTION_SET_ID);

		// then
		assertThat(result.getSolveMode()).isEqualTo(QuestionSetSolveMode.LIVE_TIME);
		assertThat(result.getTotalCount()).isEqualTo(3);
		// 101은 이후 정답이 있어도 최초 제출(오답)이 대표 → 정답으로 집계되지 않는다
		assertThat(result.getCorrectCount()).isEqualTo(1);
		assertThat(result.getScore()).isEqualTo(33.3);
		// 전 문제가 결과에 포함된다 (안 푼 103은 미응답으로 채워짐)
		assertThat(result.getResults()).hasSize(3);
		assertThat(result.getResults().get(0).getQuestionId()).isEqualTo(101L);
		assertThat(result.getResults().get(0).isCorrect()).isFalse();
		assertThat(result.getResults().get(1).getQuestionId()).isEqualTo(102L);
		assertThat(result.getResults().get(1).isCorrect()).isTrue();
		assertThat(result.getResults().get(2).getQuestionId()).isEqualTo(103L);
		assertThat(result.getResults().get(2).isCorrect()).isFalse();
		assertThat(result.getResults().get(2).getSubmittedAnswer()).isNull();
	}

	@Test
	@DisplayName("getMySolveRecord - 제출 기록이 없으면 NO_SOLVE_RECORD 예외가 발생한다")
	void getSolvingResults_throwsWhenNoRecords() {
		// given
		QuestionSetEntity questionSet = mockQuestionSet(QuestionSetSolveMode.STUDY);
		QuestionEntity question1 = mockQuestion(101L);

		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);
		when(questionReader.getOrderedQuestions(QUESTION_SET_ID)).thenReturn(List.of(question1));
		when(answerSubmitRecordReader.getEarliestByQuestionId(USER_ID, List.of(101L)))
			.thenReturn(Map.of());

		// when & then
		assertThatThrownBy(() -> solvingResultService.getSolvingResults(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(QuestionSolvingException.class)
			.satisfies(ex -> assertThat(((QuestionSolvingException)ex).getExceptionCode())
				.isEqualTo(QuestionSolveExceptionCode.NO_SOLVE_RECORD));
	}

	@Test
	@DisplayName("getMySolveRecord - 존재하지 않는 문제 셋이면 EntityNotFoundException이 발생한다")
	void getSolvingResults_throwsWhenQuestionSetNotFound() {
		// given
		when(questionSetReader.getQuestionSet(QUESTION_SET_ID))
			.thenThrow(new EntityNotFoundException(QUESTION_SET_ID + " : 해당 문제 셋을 찾을 수 없습니다."));

		// when & then
		assertThatThrownBy(() -> solvingResultService.getSolvingResults(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class);
	}

	@Test
	@DisplayName("getMySolveRecord - 팀 내 풀이 권한이 없으면 UserRoleException이 발생한다")
	void getSolvingResults_throwsWhenNoAuthority() {
		// given
		QuestionSetEntity questionSet = mockQuestionSet(QuestionSetSolveMode.STUDY);
		when(questionSetReader.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet);
		doThrow(new UserRoleException("해당 문제를 풀 수 있는 권한이 없습니다."))
			.when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);

		// when & then
		assertThatThrownBy(() -> solvingResultService.getSolvingResults(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(UserRoleException.class);
	}

	@Test
	@DisplayName("getTeamQuestionSetStatistics - 완료된 LIVE/STUDY 문제셋을 풀이날짜 최신순으로 우승자/내 정답률/평균과 함께 반환한다")
	void getTeamQuestionSetStatistics_success() {
		// given
		QuestionSetEntity liveQs = mockStatQuestionSet(100L, "실시간", QuestionSetSolveMode.LIVE_TIME,
			LocalDateTime.of(2026, 5, 31, 12, 0));
		QuestionSetEntity studyQs = mockStatQuestionSet(200L, "학습", QuestionSetSolveMode.STUDY,
			LocalDateTime.of(2026, 5, 30, 12, 0));
		when(questionSetReader.getFinishedLiveQuestionSetsBySolveModeInTeam(TEAM_ID, QuestionSetSolveMode.LIVE_TIME))
			.thenReturn(List.of(liveQs));
		when(questionSetReader.getFinishedLiveQuestionSetsBySolveModeInTeam(TEAM_ID, QuestionSetSolveMode.STUDY))
			.thenReturn(List.of(studyQs));

		QuestionEntity liveQuestion = mockQuestionOfSet(liveQs);
		QuestionEntity studyQuestion = mockQuestionOfSet(studyQs);
		when(questionEntityRepository.findAllByQuestionSetIdIn(anyList()))
			.thenReturn(List.of(liveQuestion, studyQuestion));

		QuestionSetParticipantEntity winner = mockWinner(5L, "위너", "winner");
		when(questionParticipantReader.getWinnersByQuestionSetId(anyList()))
			.thenReturn(Map.of(100L, List.of(winner)));

		Map<Long, Double> myRates = new HashMap<>();
		myRates.put(100L, 80.0);
		myRates.put(200L, null);
		when(questionSetStatisticCalculator.calculateUserCorrectRates(eq(USER_ID), anyMap())).thenReturn(myRates);
		when(questionSetStatisticCalculator.calculateOverallCorrectRates(anyMap()))
			.thenReturn(Map.of(100L, 50.0, 200L, 70.0));

		// when
		List<QuestionSetStatisticDto> result = solvingResultService.getTeamQuestionSetStatistics(MAIT_USER, TEAM_ID);

		// then: 풀이날짜 최신순 → LIVE(5/31)가 먼저
		assertThat(result).hasSize(2);
		QuestionSetStatisticDto live = result.get(0);
		assertThat(live.getQuestionSetId()).isEqualTo(100L);
		assertThat(live.getSolveMode()).isEqualTo(QuestionSetSolveMode.LIVE_TIME);
		assertThat(live.getWinners()).hasSize(1);
		assertThat(live.getWinners().get(0).getName()).isEqualTo("위너");
		assertThat(live.getWinners().get(0).getNickname()).isEqualTo("winner");
		assertThat(live.getMyCorrectRate()).isEqualTo(80.0);
		assertThat(live.getAverageCorrectRate()).isEqualTo(50.0);

		QuestionSetStatisticDto study = result.get(1);
		assertThat(study.getQuestionSetId()).isEqualTo(200L);
		assertThat(study.getWinners()).isEmpty();
		assertThat(study.getMyCorrectRate()).isNull();
		assertThat(study.getAverageCorrectRate()).isEqualTo(70.0);
	}

	@Test
	@DisplayName("getTeamQuestionSetStatistics - 완료된 문제셋이 없으면 빈 리스트를 반환한다")
	void getTeamQuestionSetStatistics_empty() {
		// given
		when(questionSetReader.getFinishedLiveQuestionSetsBySolveModeInTeam(TEAM_ID, QuestionSetSolveMode.LIVE_TIME))
			.thenReturn(List.of());
		when(questionSetReader.getFinishedLiveQuestionSetsBySolveModeInTeam(TEAM_ID, QuestionSetSolveMode.STUDY))
			.thenReturn(List.of());

		// when
		List<QuestionSetStatisticDto> result = solvingResultService.getTeamQuestionSetStatistics(MAIT_USER, TEAM_ID);

		// then
		assertThat(result).isEmpty();
		verifyNoInteractions(questionSetStatisticCalculator, questionParticipantReader, questionEntityRepository);
	}

	private QuestionSetEntity mockStatQuestionSet(final Long id, final String title, final QuestionSetSolveMode mode,
		final LocalDateTime endTime) {
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		lenient().when(questionSet.getId()).thenReturn(id);
		lenient().when(questionSet.getTitle()).thenReturn(title);
		lenient().when(questionSet.getSolveMode()).thenReturn(mode);
		lenient().when(questionSet.getEndTime()).thenReturn(endTime);
		return questionSet;
	}

	private QuestionEntity mockQuestionOfSet(final QuestionSetEntity questionSet) {
		QuestionEntity question = mock(QuestionEntity.class);
		lenient().when(question.getQuestionSet()).thenReturn(questionSet);
		return question;
	}

	private QuestionSetParticipantEntity mockWinner(final Long userId, final String name, final String nickname) {
		UserEntity user = mock(UserEntity.class);
		lenient().when(user.getId()).thenReturn(userId);
		lenient().when(user.getName()).thenReturn(name);
		lenient().when(user.getNickname()).thenReturn(nickname);
		QuestionSetParticipantEntity participant = mock(QuestionSetParticipantEntity.class);
		lenient().when(participant.getUser()).thenReturn(user);
		return participant;
	}

	private QuestionSetEntity mockQuestionSet(final QuestionSetSolveMode solveMode) {
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		lenient().when(questionSet.getId()).thenReturn(QUESTION_SET_ID);
		lenient().when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		lenient().when(questionSet.getSolveMode()).thenReturn(solveMode);
		return questionSet;
	}

	private QuestionEntity mockQuestion(final Long id) {
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(id);
		return question;
	}

	private AnswerSubmitRecordEntity record(final Long questionId, final boolean isCorrect, final Long submitOrder) {
		return AnswerSubmitRecordEntity.builder()
			.userId(USER_ID)
			.questionId(questionId)
			.isCorrect(isCorrect)
			.submitOrder(submitOrder)
			.submittedAnswer(null)
			.build();
	}
}
