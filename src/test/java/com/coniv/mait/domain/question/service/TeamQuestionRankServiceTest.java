package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.question.service.dto.PersonalAccuracyDto;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.solve.service.dto.RankDto;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.domain.user.service.component.UserReader;

@ExtendWith(MockitoExtension.class)
class TeamQuestionRankServiceTest {

	@InjectMocks
	private TeamQuestionRankService teamQuestionRankService;

	@Mock
	private QuestionScorerEntityRepository questionScorerEntityRepository;

	@Mock
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Mock
	private QuestionReader questionReader;

	@Mock
	private QuestionSetReader questionSetReader;

	@Mock
	private TeamReader teamReader;

	@Mock
	private UserReader userReader;

	@Mock
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	private UserEntity createMockUser(Long id, String name) {
		UserEntity user = mock(UserEntity.class);
		when(user.getId()).thenReturn(id);
		when(user.getEmail()).thenReturn("user" + id + "@test.com");
		when(user.getName()).thenReturn(name);
		when(user.getNickname()).thenReturn("닉네임" + id);
		when(user.getNicknameCode()).thenReturn("000" + id);
		lenient().when(user.getFullNickname()).thenReturn("닉네임" + id + "#000" + id);
		return user;
	}

	@Nested
	@DisplayName("getTeamQuestionScorerRank")
	class GetTeamQuestionScorerRank {

		@Test
		@DisplayName("득점자 랭킹 조회 성공 - 정상 케이스")
		void success() {
			// given
			Long teamId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			QuestionEntity q1 = mock(QuestionEntity.class);
			QuestionEntity q2 = mock(QuestionEntity.class);

			when(q1.getId()).thenReturn(1L);
			when(q2.getId()).thenReturn(2L);

			when(teamReader.getActiveTeam(teamId)).thenReturn(team);
			when(questionReader.getCompletedQuestionsInTeam(team)).thenReturn(List.of(q1, q2));

			QuestionScorerEntity s1 = mock(QuestionScorerEntity.class);
			QuestionScorerEntity s2 = mock(QuestionScorerEntity.class);
			QuestionScorerEntity s3 = mock(QuestionScorerEntity.class);

			when(s1.getUserId()).thenReturn(1L);
			when(s2.getUserId()).thenReturn(1L);
			when(s3.getUserId()).thenReturn(2L);

			when(questionScorerEntityRepository.findAllByQuestionIdIn(List.of(1L, 2L)))
				.thenReturn(List.of(s1, s2, s3));

			UserEntity user1 = createMockUser(1L, "가나다");
			UserEntity user2 = createMockUser(2L, "라마바");
			UserEntity user3 = createMockUser(3L, "사아자");

			when(userReader.getUsersByTeamFetchUser(team)).thenReturn(List.of(user1, user2, user3));

			// when
			List<RankDto> result = teamQuestionRankService.getTeamQuestionScorerRank(teamId);

			// then
			assertThat(result).hasSize(3);
			assertThat(result.get(0).getCount()).isEqualTo(2L);
			assertThat(result.get(0).getRank()).isEqualTo(1);
			assertThat(result.get(1).getCount()).isEqualTo(1L);
			assertThat(result.get(1).getRank()).isEqualTo(2);
			assertThat(result.get(2).getCount()).isEqualTo(0L);
			assertThat(result.get(2).getRank()).isEqualTo(3);
		}

		@Test
		@DisplayName("완료된 퀴즈가 없으면 빈 리스트 반환")
		void emptyWhenNoCompletedQuestions() {
			// given
			Long teamId = 1L;
			TeamEntity team = mock(TeamEntity.class);

			when(teamReader.getActiveTeam(teamId)).thenReturn(team);
			when(questionReader.getCompletedQuestionsInTeam(team)).thenReturn(List.of());

			// when
			List<RankDto> result = teamQuestionRankService.getTeamQuestionScorerRank(teamId);

			// then
			assertThat(result).isEmpty();
			verifyNoInteractions(questionScorerEntityRepository);
			verifyNoInteractions(userReader);
		}

		@Test
		@DisplayName("동석차 처리 - dense rank 방식으로 등수 부여")
		void denseRank() {
			// given
			Long teamId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			QuestionEntity q1 = mock(QuestionEntity.class);
			QuestionEntity q2 = mock(QuestionEntity.class);
			QuestionEntity q3 = mock(QuestionEntity.class);

			when(q1.getId()).thenReturn(1L);
			when(q2.getId()).thenReturn(2L);
			when(q3.getId()).thenReturn(3L);

			when(teamReader.getActiveTeam(teamId)).thenReturn(team);
			when(questionReader.getCompletedQuestionsInTeam(team)).thenReturn(List.of(q1, q2, q3));

			// user1: 3점, user2: 3점, user3: 1점
			QuestionScorerEntity s1 = mock(QuestionScorerEntity.class);
			QuestionScorerEntity s2 = mock(QuestionScorerEntity.class);
			QuestionScorerEntity s3 = mock(QuestionScorerEntity.class);
			QuestionScorerEntity s4 = mock(QuestionScorerEntity.class);
			QuestionScorerEntity s5 = mock(QuestionScorerEntity.class);
			QuestionScorerEntity s6 = mock(QuestionScorerEntity.class);
			QuestionScorerEntity s7 = mock(QuestionScorerEntity.class);

			when(s1.getUserId()).thenReturn(1L);
			when(s2.getUserId()).thenReturn(1L);
			when(s3.getUserId()).thenReturn(1L);
			when(s4.getUserId()).thenReturn(2L);
			when(s5.getUserId()).thenReturn(2L);
			when(s6.getUserId()).thenReturn(2L);
			when(s7.getUserId()).thenReturn(3L);

			when(questionScorerEntityRepository.findAllByQuestionIdIn(List.of(1L, 2L, 3L)))
				.thenReturn(List.of(s1, s2, s3, s4, s5, s6, s7));

			UserEntity user1 = createMockUser(1L, "가나다");
			UserEntity user2 = createMockUser(2L, "라마바");
			UserEntity user3 = createMockUser(3L, "사아자");

			when(userReader.getUsersByTeamFetchUser(team)).thenReturn(List.of(user1, user2, user3));

			// when
			List<RankDto> result = teamQuestionRankService.getTeamQuestionScorerRank(teamId);

			// then
			assertThat(result).hasSize(3);

			assertThat(result.get(0).getRank()).isEqualTo(1);
			assertThat(result.get(0).getCount()).isEqualTo(3L);
			assertThat(result.get(1).getRank()).isEqualTo(1);
			assertThat(result.get(1).getCount()).isEqualTo(3L);

			assertThat(result.get(2).getRank()).isEqualTo(2);
			assertThat(result.get(2).getCount()).isEqualTo(1L);
		}

		@Test
		@DisplayName("동석차 3그룹 - 1등 3명, 2등 2명, 3등 1명")
		void denseRankThreeGroups() {
			// given
			Long teamId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			QuestionEntity q1 = mock(QuestionEntity.class);

			when(q1.getId()).thenReturn(1L);

			when(teamReader.getActiveTeam(teamId)).thenReturn(team);
			when(questionReader.getCompletedQuestionsInTeam(team)).thenReturn(List.of(q1));

			// user1,2,3: 3점 / user4,5: 2점 / user6: 1점
			List<QuestionScorerEntity> scorers = new java.util.ArrayList<>();
			for (long userId = 1; userId <= 3; userId++) {
				for (int j = 0; j < 3; j++) {
					QuestionScorerEntity scorer = mock(QuestionScorerEntity.class);
					when(scorer.getUserId()).thenReturn(userId);
					scorers.add(scorer);
				}
			}
			for (long userId = 4; userId <= 5; userId++) {
				for (int j = 0; j < 2; j++) {
					QuestionScorerEntity scorer = mock(QuestionScorerEntity.class);
					when(scorer.getUserId()).thenReturn(userId);
					scorers.add(scorer);
				}
			}
			QuestionScorerEntity s6Only = mock(QuestionScorerEntity.class);
			when(s6Only.getUserId()).thenReturn(6L);
			scorers.add(s6Only);

			when(questionScorerEntityRepository.findAllByQuestionIdIn(List.of(1L)))
				.thenReturn(scorers);

			UserEntity user1 = createMockUser(1L, "가");
			UserEntity user2 = createMockUser(2L, "나");
			UserEntity user3 = createMockUser(3L, "다");
			UserEntity user4 = createMockUser(4L, "라");
			UserEntity user5 = createMockUser(5L, "마");
			UserEntity user6 = createMockUser(6L, "바");

			when(userReader.getUsersByTeamFetchUser(team)).thenReturn(
				List.of(user1, user2, user3, user4, user5, user6));

			// when
			List<RankDto> result = teamQuestionRankService.getTeamQuestionScorerRank(teamId);

			// then
			assertThat(result).hasSize(6);

			// 1등: 3점 (user1, user2, user3)
			assertThat(result.get(0).getRank()).isEqualTo(1);
			assertThat(result.get(1).getRank()).isEqualTo(1);
			assertThat(result.get(2).getRank()).isEqualTo(1);
			assertThat(result.get(0).getCount()).isEqualTo(3L);

			// 2등: 2점 (user4, user5)
			assertThat(result.get(3).getRank()).isEqualTo(2);
			assertThat(result.get(4).getRank()).isEqualTo(2);
			assertThat(result.get(3).getCount()).isEqualTo(2L);

			// 3등: 1점 (user6)
			assertThat(result.get(5).getRank()).isEqualTo(3);
			assertThat(result.get(5).getCount()).isEqualTo(1L);
		}
	}

	@Nested
	@DisplayName("getTeamQuestionCorrectAnswerRank")
	class GetTeamQuestionCorrectAnswerRank {
		private AnswerSubmitRecordEntity mockRecord(long userId, long questionId, boolean isCorrect) {
			AnswerSubmitRecordEntity record = mock(AnswerSubmitRecordEntity.class);
			when(record.getUserId()).thenReturn(userId);
			when(record.getQuestionId()).thenReturn(questionId);
			when(record.isCorrect()).thenReturn(isCorrect);
			return record;
		}

		@Test
		@DisplayName("정답자 랭킹 조회 성공 - 정상 케이스")
		void success() {
			// given
			Long teamId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			QuestionEntity q1 = mock(QuestionEntity.class);
			QuestionEntity q2 = mock(QuestionEntity.class);

			when(q1.getId()).thenReturn(1L);
			when(q2.getId()).thenReturn(2L);

			when(teamReader.getActiveTeam(teamId)).thenReturn(team);
			when(questionReader.getCompletedQuestionsInTeam(team)).thenReturn(List.of(q1, q2));

			AnswerSubmitRecordEntity a1 = mockRecord(1L, 1L, true);
			AnswerSubmitRecordEntity a2 = mockRecord(1L, 2L, true);
			AnswerSubmitRecordEntity a3 = mockRecord(2L, 1L, true);

			when(answerSubmitRecordEntityRepository.findAllByQuestionIdIn(List.of(1L, 2L)))
				.thenReturn(List.of(a1, a2, a3));

			UserEntity user1 = createMockUser(1L, "가나다");
			UserEntity user2 = createMockUser(2L, "라마바");

			when(userReader.getUsersByTeamFetchUser(team)).thenReturn(List.of(user1, user2));

			// when
			List<RankDto> result = teamQuestionRankService.getTeamQuestionCorrectAnswerRank(teamId);

			// then
			assertThat(result).hasSize(2);
			assertThat(result.get(0).getCount()).isEqualTo(2L);
			assertThat(result.get(0).getRank()).isEqualTo(1);
			assertThat(result.get(1).getCount()).isEqualTo(1L);
			assertThat(result.get(1).getRank()).isEqualTo(2);
		}

		@Test
		@DisplayName("동일 문제에 오답 기록이 있으면 해당 문제는 오답으로 처리")
		void questionWithAnyWrongAnswerIsWrong() {
			Long teamId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			QuestionEntity q1 = mock(QuestionEntity.class);
			QuestionEntity q2 = mock(QuestionEntity.class);

			when(q1.getId()).thenReturn(1L);
			when(q2.getId()).thenReturn(2L);

			when(teamReader.getActiveTeam(teamId)).thenReturn(team);
			when(questionReader.getCompletedQuestionsInTeam(team)).thenReturn(List.of(q1, q2));

			List<AnswerSubmitRecordEntity> records = List.of(
				mockRecord(1L, 1L, true),
				mockRecord(1L, 1L, false),
				mockRecord(1L, 2L, true),
				mockRecord(2L, 1L, true),
				mockRecord(2L, 2L, true));

			when(answerSubmitRecordEntityRepository.findAllByQuestionIdIn(List.of(1L, 2L)))
				.thenReturn(records);

			UserEntity user1 = createMockUser(1L, "가나다");
			UserEntity user2 = createMockUser(2L, "라마바");
			UserEntity user3 = createMockUser(3L, "사아자");
			when(userReader.getUsersByTeamFetchUser(team)).thenReturn(List.of(user1, user2, user3));

			List<RankDto> result = teamQuestionRankService.getTeamQuestionCorrectAnswerRank(teamId);

			assertThat(result).hasSize(3);
			assertThat(result.get(0).getUser().getId()).isEqualTo(2L);
			assertThat(result.get(0).getCount()).isEqualTo(2L);
			assertThat(result.get(1).getUser().getId()).isEqualTo(1L);
			assertThat(result.get(1).getCount()).isEqualTo(1L);
			assertThat(result.get(2).getUser().getId()).isEqualTo(3L);
			assertThat(result.get(2).getCount()).isEqualTo(0L);
			assertThat(result.get(2).getRank()).isEqualTo(3);
		}

		@Test
		@DisplayName("완료된 퀴즈가 없으면 빈 리스트 반환")
		void emptyWhenNoCompletedQuestions() {
			// given
			Long teamId = 1L;
			TeamEntity team = mock(TeamEntity.class);

			when(teamReader.getActiveTeam(teamId)).thenReturn(team);
			when(questionReader.getCompletedQuestionsInTeam(team)).thenReturn(List.of());

			// when
			List<RankDto> result = teamQuestionRankService.getTeamQuestionCorrectAnswerRank(teamId);

			// then
			assertThat(result).isEmpty();
			verifyNoInteractions(answerSubmitRecordEntityRepository);
			verifyNoInteractions(userReader);
		}

		@Test
		@DisplayName("동석차 처리 - dense rank 방식으로 등수 부여")
		void denseRank() {
			// given
			Long teamId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			QuestionEntity q1 = mock(QuestionEntity.class);
			QuestionEntity q2 = mock(QuestionEntity.class);

			when(q1.getId()).thenReturn(1L);
			when(q2.getId()).thenReturn(2L);

			when(teamReader.getActiveTeam(teamId)).thenReturn(team);
			when(questionReader.getCompletedQuestionsInTeam(team)).thenReturn(List.of(q1, q2));

			// user1: 2개, user2: 2개, user3: 1개
			AnswerSubmitRecordEntity a1 = mockRecord(1L, 1L, true);
			AnswerSubmitRecordEntity a2 = mockRecord(1L, 2L, true);
			AnswerSubmitRecordEntity a3 = mockRecord(2L, 1L, true);
			AnswerSubmitRecordEntity a4 = mockRecord(2L, 2L, true);
			AnswerSubmitRecordEntity a5 = mockRecord(3L, 1L, true);

			when(answerSubmitRecordEntityRepository.findAllByQuestionIdIn(List.of(1L, 2L)))
				.thenReturn(List.of(a1, a2, a3, a4, a5));

			UserEntity user1 = createMockUser(1L, "가나다");
			UserEntity user2 = createMockUser(2L, "라마바");
			UserEntity user3 = createMockUser(3L, "사아자");

			when(userReader.getUsersByTeamFetchUser(team)).thenReturn(List.of(user1, user2, user3));

			// when
			List<RankDto> result = teamQuestionRankService.getTeamQuestionCorrectAnswerRank(teamId);

			// then
			assertThat(result).hasSize(3);

			// 1등: 2개 (user1, user2)
			assertThat(result.get(0).getRank()).isEqualTo(1);
			assertThat(result.get(0).getCount()).isEqualTo(2L);
			assertThat(result.get(1).getRank()).isEqualTo(1);
			assertThat(result.get(1).getCount()).isEqualTo(2L);

			// 2등: 1개 (user3) - dense rank이므로 2등
			assertThat(result.get(2).getRank()).isEqualTo(2);
			assertThat(result.get(2).getCount()).isEqualTo(1L);
		}

		@Test
		@DisplayName("동석차 3그룹 - 1등 3명, 2등 5명, 3등 6명이면 14명 전부 반환")
		void denseRankThreeGroupsAllReturned() {
			// given
			Long teamId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			List<QuestionEntity> questions = new java.util.ArrayList<>();
			List<Long> questionIds = new java.util.ArrayList<>();
			for (long questionId = 1L; questionId <= 10L; questionId++) {
				QuestionEntity question = mock(QuestionEntity.class);
				when(question.getId()).thenReturn(questionId);
				questions.add(question);
				questionIds.add(questionId);
			}

			when(teamReader.getActiveTeam(teamId)).thenReturn(team);
			when(questionReader.getCompletedQuestionsInTeam(team)).thenReturn(questions);

			// 1등(10개): user1~3, 2등(8개): user4~8, 3등(5개): user9~14
			List<AnswerSubmitRecordEntity> records = new java.util.ArrayList<>();
			for (long userId = 1; userId <= 3; userId++) {
				for (long questionId = 1; questionId <= 10; questionId++) {
					records.add(mockRecord(userId, questionId, true));
				}
			}
			for (long userId = 4; userId <= 8; userId++) {
				for (long questionId = 1; questionId <= 8; questionId++) {
					records.add(mockRecord(userId, questionId, true));
				}
			}
			for (long userId = 9; userId <= 14; userId++) {
				for (long questionId = 1; questionId <= 5; questionId++) {
					records.add(mockRecord(userId, questionId, true));
				}
			}

			when(answerSubmitRecordEntityRepository.findAllByQuestionIdIn(questionIds))
				.thenReturn(records);

			List<UserEntity> teamUsers = new java.util.ArrayList<>();
			for (long userId = 1; userId <= 14; userId++) {
				UserEntity user = createMockUser(userId, "유저" + userId);
				teamUsers.add(user);
			}
			when(userReader.getUsersByTeamFetchUser(team)).thenReturn(teamUsers);

			// when
			List<RankDto> result = teamQuestionRankService.getTeamQuestionCorrectAnswerRank(teamId);

			// then
			assertThat(result).hasSize(14);

			long rank1Count = result.stream().filter(r -> r.getRank() == 1).count();
			long rank2Count = result.stream().filter(r -> r.getRank() == 2).count();
			long rank3Count = result.stream().filter(r -> r.getRank() == 3).count();

			assertThat(rank1Count).isEqualTo(3);
			assertThat(rank2Count).isEqualTo(5);
			assertThat(rank3Count).isEqualTo(6);

			assertThat(result.stream().filter(r -> r.getRank() == 1).allMatch(r -> r.getCount() == 10)).isTrue();
			assertThat(result.stream().filter(r -> r.getRank() == 2).allMatch(r -> r.getCount() == 8)).isTrue();
			assertThat(result.stream().filter(r -> r.getRank() == 3).allMatch(r -> r.getCount() == 5)).isTrue();
		}

		@Test
		@DisplayName("전원 동점이면 전원 1등")
		void allSameScore() {
			// given
			Long teamId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			QuestionEntity q1 = mock(QuestionEntity.class);

			when(q1.getId()).thenReturn(1L);

			when(teamReader.getActiveTeam(teamId)).thenReturn(team);
			when(questionReader.getCompletedQuestionsInTeam(team)).thenReturn(List.of(q1));

			AnswerSubmitRecordEntity a1 = mockRecord(1L, 1L, true);
			AnswerSubmitRecordEntity a2 = mockRecord(2L, 1L, true);
			AnswerSubmitRecordEntity a3 = mockRecord(3L, 1L, true);

			when(answerSubmitRecordEntityRepository.findAllByQuestionIdIn(List.of(1L)))
				.thenReturn(List.of(a1, a2, a3));

			UserEntity user1 = createMockUser(1L, "가");
			UserEntity user2 = createMockUser(2L, "나");
			UserEntity user3 = createMockUser(3L, "다");

			when(userReader.getUsersByTeamFetchUser(team)).thenReturn(List.of(user1, user2, user3));

			// when
			List<RankDto> result = teamQuestionRankService.getTeamQuestionCorrectAnswerRank(teamId);

			// then
			assertThat(result).hasSize(3);
			assertThat(result).allMatch(r -> r.getRank() == 1);
			assertThat(result).allMatch(r -> r.getCount() == 1L);
		}
	}

	@Nested
	@DisplayName("getPersonalAccuracy")
	class GetPersonalAccuracy {

		@Test
		@DisplayName("개인 정답률 조회 성공 - 실시간 문제만 있는 경우")
		void success() {
			// given
			Long teamId = 1L;
			Long userId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			when(team.getId()).thenReturn(teamId);
			when(teamReader.getActiveTeam(teamId)).thenReturn(team);

			QuestionSetEntity liveQs = mock(QuestionSetEntity.class);
			when(liveQs.getId()).thenReturn(100L);
			when(questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId)).thenReturn(List.of(liveQs));

			when(solvingSessionEntityRepository.findAllByUserIdAndStatusAndSolveModeAndQuestionSetTeamId(
				userId, SolvingStatus.COMPLETE, QuestionSetSolveMode.STUDY, teamId)).thenReturn(List.of());

			QuestionEntity q1 = mock(QuestionEntity.class);
			QuestionEntity q2 = mock(QuestionEntity.class);
			QuestionEntity q3 = mock(QuestionEntity.class);
			when(q1.getId()).thenReturn(1L);
			when(q2.getId()).thenReturn(2L);
			when(q3.getId()).thenReturn(3L);

			when(questionEntityRepository.findAllByQuestionSetIdIn(List.of(100L)))
				.thenReturn(List.of(q1, q2, q3));

			AnswerSubmitRecordEntity a1 = mock(AnswerSubmitRecordEntity.class);
			AnswerSubmitRecordEntity a2 = mock(AnswerSubmitRecordEntity.class);
			AnswerSubmitRecordEntity a3 = mock(AnswerSubmitRecordEntity.class);
			when(a1.getQuestionId()).thenReturn(1L);
			when(a1.isCorrect()).thenReturn(true);
			when(a2.getQuestionId()).thenReturn(2L);
			when(a2.isCorrect()).thenReturn(true);
			when(a3.getQuestionId()).thenReturn(3L);
			when(a3.isCorrect()).thenReturn(false);

			when(answerSubmitRecordEntityRepository.findAllByUserIdAndQuestionIdIn(eq(userId), anyList()))
				.thenReturn(List.of(a1, a2, a3));

			// when
			PersonalAccuracyDto result = teamQuestionRankService.getPersonalAccuracy(teamId, userId);

			// then
			assertThat(result.getTotalSolvedCount()).isEqualTo(3);
			assertThat(result.getCorrectCount()).isEqualTo(2);
			assertThat(result.getAccuracyRate()).isEqualTo(66.7);
		}

		@Test
		@DisplayName("실시간 + 학습모드 문제를 합산하여 정답률 계산")
		void combineLiveAndStudyQuestions() {
			// given
			Long teamId = 1L;
			Long userId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			when(team.getId()).thenReturn(teamId);
			when(teamReader.getActiveTeam(teamId)).thenReturn(team);

			QuestionSetEntity liveQs = mock(QuestionSetEntity.class);
			when(liveQs.getId()).thenReturn(100L);
			when(questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId)).thenReturn(List.of(liveQs));

			QuestionSetEntity studyQs = mock(QuestionSetEntity.class);
			when(studyQs.getId()).thenReturn(200L);
			SolvingSessionEntity session = mock(SolvingSessionEntity.class);
			when(session.getQuestionSet()).thenReturn(studyQs);

			when(solvingSessionEntityRepository.findAllByUserIdAndStatusAndSolveModeAndQuestionSetTeamId(
				userId, SolvingStatus.COMPLETE, QuestionSetSolveMode.STUDY, teamId)).thenReturn(List.of(session));

			QuestionEntity liveQ1 = mock(QuestionEntity.class);
			when(liveQ1.getId()).thenReturn(1L);
			QuestionEntity studyQ1 = mock(QuestionEntity.class);
			QuestionEntity studyQ2 = mock(QuestionEntity.class);
			when(studyQ1.getId()).thenReturn(2L);
			when(studyQ2.getId()).thenReturn(3L);

			when(questionEntityRepository.findAllByQuestionSetIdIn(anyList()))
				.thenReturn(List.of(liveQ1, studyQ1, studyQ2));

			AnswerSubmitRecordEntity a1 = mock(AnswerSubmitRecordEntity.class);
			AnswerSubmitRecordEntity a2 = mock(AnswerSubmitRecordEntity.class);
			AnswerSubmitRecordEntity a3 = mock(AnswerSubmitRecordEntity.class);
			when(a1.getQuestionId()).thenReturn(1L);
			when(a1.isCorrect()).thenReturn(true);
			when(a2.getQuestionId()).thenReturn(2L);
			when(a2.isCorrect()).thenReturn(true);
			when(a3.getQuestionId()).thenReturn(3L);
			when(a3.isCorrect()).thenReturn(false);

			when(answerSubmitRecordEntityRepository.findAllByUserIdAndQuestionIdIn(eq(userId), anyList()))
				.thenReturn(List.of(a1, a2, a3));

			// when
			PersonalAccuracyDto result = teamQuestionRankService.getPersonalAccuracy(teamId, userId);

			// then
			assertThat(result.getTotalSolvedCount()).isEqualTo(3);
			assertThat(result.getCorrectCount()).isEqualTo(2);
			assertThat(result.getAccuracyRate()).isEqualTo(66.7);
		}

		@Test
		@DisplayName("학습모드 문제만 있는 경우에도 정상 계산")
		void studyModeOnly() {
			// given
			Long teamId = 1L;
			Long userId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			when(team.getId()).thenReturn(teamId);
			when(teamReader.getActiveTeam(teamId)).thenReturn(team);

			when(questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId)).thenReturn(List.of());

			QuestionSetEntity studyQs = mock(QuestionSetEntity.class);
			when(studyQs.getId()).thenReturn(200L);
			SolvingSessionEntity session = mock(SolvingSessionEntity.class);
			when(session.getQuestionSet()).thenReturn(studyQs);

			when(solvingSessionEntityRepository.findAllByUserIdAndStatusAndSolveModeAndQuestionSetTeamId(
				userId, SolvingStatus.COMPLETE, QuestionSetSolveMode.STUDY, teamId)).thenReturn(List.of(session));

			QuestionEntity studyQ1 = mock(QuestionEntity.class);
			when(studyQ1.getId()).thenReturn(1L);

			when(questionEntityRepository.findAllByQuestionSetIdIn(List.of(200L)))
				.thenReturn(List.of(studyQ1));

			AnswerSubmitRecordEntity a1 = mock(AnswerSubmitRecordEntity.class);
			when(a1.getQuestionId()).thenReturn(1L);
			when(a1.isCorrect()).thenReturn(true);

			when(answerSubmitRecordEntityRepository.findAllByUserIdAndQuestionIdIn(eq(userId), anyList()))
				.thenReturn(List.of(a1));

			// when
			PersonalAccuracyDto result = teamQuestionRankService.getPersonalAccuracy(teamId, userId);

			// then
			assertThat(result.getTotalSolvedCount()).isEqualTo(1);
			assertThat(result.getCorrectCount()).isEqualTo(1);
			assertThat(result.getAccuracyRate()).isEqualTo(100.0);
		}

		@Test
		@DisplayName("완료된 퀴즈가 없으면 모두 0 반환")
		void emptyWhenNoCompletedQuestions() {
			// given
			Long teamId = 1L;
			Long userId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			when(team.getId()).thenReturn(teamId);
			when(teamReader.getActiveTeam(teamId)).thenReturn(team);

			when(questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId)).thenReturn(List.of());
			when(solvingSessionEntityRepository.findAllByUserIdAndStatusAndSolveModeAndQuestionSetTeamId(
				userId, SolvingStatus.COMPLETE, QuestionSetSolveMode.STUDY, teamId)).thenReturn(List.of());
			when(questionEntityRepository.findAllByQuestionSetIdIn(anyList())).thenReturn(List.of());

			// when
			PersonalAccuracyDto result = teamQuestionRankService.getPersonalAccuracy(teamId, userId);

			// then
			assertThat(result.getTotalSolvedCount()).isZero();
			assertThat(result.getCorrectCount()).isZero();
			assertThat(result.getAccuracyRate()).isZero();
			verifyNoInteractions(answerSubmitRecordEntityRepository);
		}

		@Test
		@DisplayName("유저가 푼 기록이 없으면 모두 0 반환")
		void emptyWhenNoUserRecords() {
			// given
			Long teamId = 1L;
			Long userId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			when(team.getId()).thenReturn(teamId);
			when(teamReader.getActiveTeam(teamId)).thenReturn(team);

			QuestionSetEntity liveQs = mock(QuestionSetEntity.class);
			when(liveQs.getId()).thenReturn(100L);
			when(questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId)).thenReturn(List.of(liveQs));
			when(solvingSessionEntityRepository.findAllByUserIdAndStatusAndSolveModeAndQuestionSetTeamId(
				userId, SolvingStatus.COMPLETE, QuestionSetSolveMode.STUDY, teamId)).thenReturn(List.of());

			QuestionEntity q1 = mock(QuestionEntity.class);
			when(q1.getId()).thenReturn(1L);
			when(questionEntityRepository.findAllByQuestionSetIdIn(List.of(100L))).thenReturn(List.of(q1));

			when(answerSubmitRecordEntityRepository.findAllByUserIdAndQuestionIdIn(eq(userId), anyList()))
				.thenReturn(List.of());

			// when
			PersonalAccuracyDto result = teamQuestionRankService.getPersonalAccuracy(teamId, userId);

			// then
			assertThat(result.getTotalSolvedCount()).isZero();
			assertThat(result.getCorrectCount()).isZero();
			assertThat(result.getAccuracyRate()).isZero();
		}

		@Test
		@DisplayName("같은 문제를 여러 번 제출해도 DISTINCT하게 1개로 카운트")
		void distinctCountForSameQuestion() {
			// given
			Long teamId = 1L;
			Long userId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			when(team.getId()).thenReturn(teamId);
			when(teamReader.getActiveTeam(teamId)).thenReturn(team);

			QuestionSetEntity liveQs = mock(QuestionSetEntity.class);
			when(liveQs.getId()).thenReturn(100L);
			when(questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId)).thenReturn(List.of(liveQs));
			when(solvingSessionEntityRepository.findAllByUserIdAndStatusAndSolveModeAndQuestionSetTeamId(
				userId, SolvingStatus.COMPLETE, QuestionSetSolveMode.STUDY, teamId)).thenReturn(List.of());

			QuestionEntity q1 = mock(QuestionEntity.class);
			when(q1.getId()).thenReturn(1L);
			when(questionEntityRepository.findAllByQuestionSetIdIn(List.of(100L))).thenReturn(List.of(q1));

			AnswerSubmitRecordEntity a1 = mock(AnswerSubmitRecordEntity.class);
			AnswerSubmitRecordEntity a2 = mock(AnswerSubmitRecordEntity.class);
			AnswerSubmitRecordEntity a3 = mock(AnswerSubmitRecordEntity.class);
			when(a1.getQuestionId()).thenReturn(1L);
			when(a1.isCorrect()).thenReturn(false);
			when(a2.getQuestionId()).thenReturn(1L);
			when(a2.isCorrect()).thenReturn(false);
			when(a3.getQuestionId()).thenReturn(1L);
			when(a3.isCorrect()).thenReturn(true);

			when(answerSubmitRecordEntityRepository.findAllByUserIdAndQuestionIdIn(eq(userId), anyList()))
				.thenReturn(List.of(a1, a2, a3));

			// when
			PersonalAccuracyDto result = teamQuestionRankService.getPersonalAccuracy(teamId, userId);

			// then
			assertThat(result.getTotalSolvedCount()).isEqualTo(1);
			assertThat(result.getCorrectCount()).isEqualTo(1);
			assertThat(result.getAccuracyRate()).isEqualTo(100.0);
		}

		@Test
		@DisplayName("재제출로 정답 처리 - 오답 후 정답이면 맞은 문제로 카운트")
		void retryCorrectAnswer() {
			// given
			Long teamId = 1L;
			Long userId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			when(team.getId()).thenReturn(teamId);
			when(teamReader.getActiveTeam(teamId)).thenReturn(team);

			QuestionSetEntity liveQs = mock(QuestionSetEntity.class);
			when(liveQs.getId()).thenReturn(100L);
			when(questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId)).thenReturn(List.of(liveQs));
			when(solvingSessionEntityRepository.findAllByUserIdAndStatusAndSolveModeAndQuestionSetTeamId(
				userId, SolvingStatus.COMPLETE, QuestionSetSolveMode.STUDY, teamId)).thenReturn(List.of());

			QuestionEntity q1 = mock(QuestionEntity.class);
			QuestionEntity q2 = mock(QuestionEntity.class);
			when(q1.getId()).thenReturn(1L);
			when(q2.getId()).thenReturn(2L);
			when(questionEntityRepository.findAllByQuestionSetIdIn(List.of(100L))).thenReturn(List.of(q1, q2));

			AnswerSubmitRecordEntity a1 = mock(AnswerSubmitRecordEntity.class);
			AnswerSubmitRecordEntity a2 = mock(AnswerSubmitRecordEntity.class);
			when(a1.getQuestionId()).thenReturn(1L);
			when(a1.isCorrect()).thenReturn(false);
			when(a2.getQuestionId()).thenReturn(1L);
			when(a2.isCorrect()).thenReturn(true);

			AnswerSubmitRecordEntity a3 = mock(AnswerSubmitRecordEntity.class);
			when(a3.getQuestionId()).thenReturn(2L);
			when(a3.isCorrect()).thenReturn(false);

			when(answerSubmitRecordEntityRepository.findAllByUserIdAndQuestionIdIn(eq(userId), anyList()))
				.thenReturn(List.of(a1, a2, a3));

			// when
			PersonalAccuracyDto result = teamQuestionRankService.getPersonalAccuracy(teamId, userId);

			// then
			assertThat(result.getTotalSolvedCount()).isEqualTo(2);
			assertThat(result.getCorrectCount()).isEqualTo(1);
			assertThat(result.getAccuracyRate()).isEqualTo(50.0);
		}

		@Test
		@DisplayName("전부 오답이면 정답률 0.0")
		void allWrong() {
			// given
			Long teamId = 1L;
			Long userId = 1L;
			TeamEntity team = mock(TeamEntity.class);
			when(team.getId()).thenReturn(teamId);
			when(teamReader.getActiveTeam(teamId)).thenReturn(team);

			QuestionSetEntity liveQs = mock(QuestionSetEntity.class);
			when(liveQs.getId()).thenReturn(100L);
			when(questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId)).thenReturn(List.of(liveQs));
			when(solvingSessionEntityRepository.findAllByUserIdAndStatusAndSolveModeAndQuestionSetTeamId(
				userId, SolvingStatus.COMPLETE, QuestionSetSolveMode.STUDY, teamId)).thenReturn(List.of());

			QuestionEntity q1 = mock(QuestionEntity.class);
			QuestionEntity q2 = mock(QuestionEntity.class);
			when(q1.getId()).thenReturn(1L);
			when(q2.getId()).thenReturn(2L);
			when(questionEntityRepository.findAllByQuestionSetIdIn(List.of(100L))).thenReturn(List.of(q1, q2));

			AnswerSubmitRecordEntity a1 = mock(AnswerSubmitRecordEntity.class);
			AnswerSubmitRecordEntity a2 = mock(AnswerSubmitRecordEntity.class);
			when(a1.getQuestionId()).thenReturn(1L);
			when(a1.isCorrect()).thenReturn(false);
			when(a2.getQuestionId()).thenReturn(2L);
			when(a2.isCorrect()).thenReturn(false);

			when(answerSubmitRecordEntityRepository.findAllByUserIdAndQuestionIdIn(eq(userId), anyList()))
				.thenReturn(List.of(a1, a2));

			// when
			PersonalAccuracyDto result = teamQuestionRankService.getPersonalAccuracy(teamId, userId);

			// then
			assertThat(result.getTotalSolvedCount()).isEqualTo(2);
			assertThat(result.getCorrectCount()).isZero();
			assertThat(result.getAccuracyRate()).isEqualTo(0.0);
		}

		@Test
		@DisplayName("팀 멤버가 아닌 유저가 조회하면 예외 발생")
		void throwExceptionWhenNotTeamMember() {
			// given
			Long teamId = 1L;
			Long userId = 999L;

			doThrow(new UserRoleException("해당 팀의 멤버가 아닙니다."))
				.when(teamRoleValidator).checkIsTeamMember(teamId, userId);

			// when & then
			assertThatThrownBy(() -> teamQuestionRankService.getPersonalAccuracy(teamId, userId))
				.isInstanceOf(UserRoleException.class)
				.hasMessage("해당 팀의 멤버가 아닙니다.");

			verifyNoInteractions(questionSetReader);
			verifyNoInteractions(answerSubmitRecordEntityRepository);
		}
	}
}
