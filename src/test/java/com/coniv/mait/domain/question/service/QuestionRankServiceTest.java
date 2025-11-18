package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.dto.AnswerRankDto;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.dto.ParticipantCorrectAnswerRankDto;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.domain.user.service.dto.UserDto;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionRankServiceTest {

	@InjectMocks
	private QuestionRankService questionRankService;

	@Mock
	private QuestionSetParticipantRepository questionSetParticipantRepository;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Mock
	private UserReader userReader;

	@Mock
	private TeamReader teamReader;

	@Mock
	private QuestionReader questionReader;

	@Test
	@DisplayName("참가자 정답 랭킹 조회 성공 - 활성/탈락 참가자 분류 및 정답 수 기준 정렬")
	void getParticipantCorrectRank_Success() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		UserEntity user1 = mock(UserEntity.class);
		UserEntity user2 = mock(UserEntity.class);
		UserEntity user3 = mock(UserEntity.class);

		when(user1.getId()).thenReturn(1L);
		when(user2.getId()).thenReturn(2L);
		when(user3.getId()).thenReturn(3L);

		QuestionSetParticipantEntity participant1 = mock(QuestionSetParticipantEntity.class);
		QuestionSetParticipantEntity participant2 = mock(QuestionSetParticipantEntity.class);
		QuestionSetParticipantEntity participant3 = mock(QuestionSetParticipantEntity.class);

		when(participant1.getId()).thenReturn(1L);
		when(participant1.getUser()).thenReturn(user1);
		when(participant1.getParticipantName()).thenReturn("참가자1");
		when(participant1.getStatus()).thenReturn(ParticipantStatus.ACTIVE);

		when(participant2.getId()).thenReturn(2L);
		when(participant2.getUser()).thenReturn(user2);
		when(participant2.getParticipantName()).thenReturn("참가자2");
		when(participant2.getStatus()).thenReturn(ParticipantStatus.ACTIVE);

		when(participant3.getId()).thenReturn(3L);
		when(participant3.getUser()).thenReturn(user3);
		when(participant3.getParticipantName()).thenReturn("참가자3");
		when(participant3.getStatus()).thenReturn(ParticipantStatus.ELIMINATED);

		QuestionEntity question1 = mock(QuestionEntity.class);
		QuestionEntity question2 = mock(QuestionEntity.class);
		QuestionEntity question3 = mock(QuestionEntity.class);

		when(question1.getId()).thenReturn(1L);
		when(question2.getId()).thenReturn(2L);
		when(question3.getId()).thenReturn(3L);

		// user1: 3개 정답, user2: 1개 정답, user3: 2개 정답
		AnswerSubmitRecordEntity answer1 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer2 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer3 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer4 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer5 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer6 = mock(AnswerSubmitRecordEntity.class);

		when(answer1.getUserId()).thenReturn(1L);
		when(answer1.getQuestionId()).thenReturn(1L);
		when(answer2.getUserId()).thenReturn(1L);
		when(answer2.getQuestionId()).thenReturn(2L);
		when(answer3.getUserId()).thenReturn(1L);
		when(answer3.getQuestionId()).thenReturn(3L);
		when(answer4.getUserId()).thenReturn(2L);
		when(answer4.getQuestionId()).thenReturn(1L);
		when(answer5.getUserId()).thenReturn(3L);
		when(answer5.getQuestionId()).thenReturn(1L);
		when(answer6.getUserId()).thenReturn(3L);
		when(answer6.getQuestionId()).thenReturn(2L);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet))
			.thenReturn(List.of(participant1, participant2, participant3));
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId))
			.thenReturn(List.of(question1, question2, question3));
		when(answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(
			List.of(1L, 2L, 3L), true))
			.thenReturn(List.of(answer1, answer2, answer3, answer4, answer5, answer6));

		// when
		ParticipantCorrectAnswerRankDto result = questionRankService.getParticipantCorrectRank(questionSetId);

		// then
		assertNotNull(result);

		// ACTIVE 참가자 검증 (정답 수 내림차순 정렬)
		assertThat(result.getActiveParticipants()).hasSize(2);
		assertThat(result.getActiveParticipants().get(0).getParticipantDto().getUserId()).isEqualTo(1L);
		assertThat(result.getActiveParticipants().get(0).getCorrectAnswerCount()).isEqualTo(3L);
		assertThat(result.getActiveParticipants().get(1).getParticipantDto().getUserId()).isEqualTo(2L);
		assertThat(result.getActiveParticipants().get(1).getCorrectAnswerCount()).isEqualTo(1L);

		// ELIMINATED 참가자 검증
		assertThat(result.getEliminatedParticipants()).hasSize(1);
		assertThat(result.getEliminatedParticipants().get(0).getParticipantDto().getUserId()).isEqualTo(3L);
		assertThat(result.getEliminatedParticipants().get(0).getCorrectAnswerCount()).isEqualTo(2L);

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionSetParticipantRepository).findAllByQuestionSetWithFetchJoinUser(questionSet);
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(answerSubmitRecordEntityRepository).findAllByQuestionIdInAndIsCorrect(List.of(1L, 2L, 3L), true);
	}

	@Test
	@DisplayName("참가자 정답 랭킹 조회 성공 - 참가자가 없는 경우")
	void getParticipantCorrectRank_NoParticipants() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		QuestionEntity question1 = mock(QuestionEntity.class);

		when(question1.getId()).thenReturn(1L);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet))
			.thenReturn(List.of());
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId))
			.thenReturn(List.of(question1));
		when(answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(List.of(1L), true))
			.thenReturn(List.of());

		// when
		ParticipantCorrectAnswerRankDto result = questionRankService.getParticipantCorrectRank(questionSetId);

		// then
		assertNotNull(result);
		assertThat(result.getActiveParticipants()).isEmpty();
		assertThat(result.getEliminatedParticipants()).isEmpty();

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionSetParticipantRepository).findAllByQuestionSetWithFetchJoinUser(questionSet);
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(answerSubmitRecordEntityRepository).findAllByQuestionIdInAndIsCorrect(List.of(1L), true);
	}

	@Test
	@DisplayName("참가자 정답 랭킹 조회 성공 - 정답 기록이 없는 참가자들")
	void getParticipantCorrectRank_NoCorrectAnswers() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		UserEntity user1 = mock(UserEntity.class);
		UserEntity user2 = mock(UserEntity.class);

		when(user1.getId()).thenReturn(1L);
		when(user2.getId()).thenReturn(2L);

		QuestionSetParticipantEntity participant1 = mock(QuestionSetParticipantEntity.class);
		QuestionSetParticipantEntity participant2 = mock(QuestionSetParticipantEntity.class);

		when(participant1.getId()).thenReturn(1L);
		when(participant1.getUser()).thenReturn(user1);
		when(participant1.getParticipantName()).thenReturn("참가자1");
		when(participant1.getStatus()).thenReturn(ParticipantStatus.ACTIVE);

		when(participant2.getId()).thenReturn(2L);
		when(participant2.getUser()).thenReturn(user2);
		when(participant2.getParticipantName()).thenReturn("참가자2");
		when(participant2.getStatus()).thenReturn(ParticipantStatus.ACTIVE);

		QuestionEntity question1 = mock(QuestionEntity.class);
		when(question1.getId()).thenReturn(1L);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet))
			.thenReturn(List.of(participant1, participant2));
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId))
			.thenReturn(List.of(question1));
		when(answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(List.of(1L), true))
			.thenReturn(List.of());

		// when
		ParticipantCorrectAnswerRankDto result = questionRankService.getParticipantCorrectRank(questionSetId);

		// then
		assertNotNull(result);
		assertThat(result.getActiveParticipants()).hasSize(2);
		assertThat(result.getActiveParticipants().get(0).getCorrectAnswerCount()).isEqualTo(0L);
		assertThat(result.getActiveParticipants().get(1).getCorrectAnswerCount()).isEqualTo(0L);
		assertThat(result.getEliminatedParticipants()).isEmpty();

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionSetParticipantRepository).findAllByQuestionSetWithFetchJoinUser(questionSet);
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(answerSubmitRecordEntityRepository).findAllByQuestionIdInAndIsCorrect(List.of(1L), true);
	}

	@Test
	@DisplayName("참가자 정답 랭킹 조회 성공 - 동일한 정답 수를 가진 참가자들")
	void getParticipantCorrectRank_SameCorrectCount() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		UserEntity user1 = mock(UserEntity.class);
		UserEntity user2 = mock(UserEntity.class);

		when(user1.getId()).thenReturn(1L);
		when(user2.getId()).thenReturn(2L);

		QuestionSetParticipantEntity participant1 = mock(QuestionSetParticipantEntity.class);
		QuestionSetParticipantEntity participant2 = mock(QuestionSetParticipantEntity.class);

		when(participant1.getId()).thenReturn(1L);
		when(participant1.getUser()).thenReturn(user1);
		when(participant1.getParticipantName()).thenReturn("참가자1");
		when(participant1.getStatus()).thenReturn(ParticipantStatus.ACTIVE);

		when(participant2.getId()).thenReturn(2L);
		when(participant2.getUser()).thenReturn(user2);
		when(participant2.getParticipantName()).thenReturn("참가자2");
		when(participant2.getStatus()).thenReturn(ParticipantStatus.ACTIVE);

		QuestionEntity question1 = mock(QuestionEntity.class);
		QuestionEntity question2 = mock(QuestionEntity.class);

		when(question1.getId()).thenReturn(1L);
		when(question2.getId()).thenReturn(2L);

		// 두 참가자 모두 1개씩 정답
		AnswerSubmitRecordEntity answer1 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer2 = mock(AnswerSubmitRecordEntity.class);

		when(answer1.getUserId()).thenReturn(1L);
		when(answer1.getQuestionId()).thenReturn(1L);
		when(answer2.getUserId()).thenReturn(2L);
		when(answer2.getQuestionId()).thenReturn(2L);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet))
			.thenReturn(List.of(participant1, participant2));
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId))
			.thenReturn(List.of(question1, question2));
		when(answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(List.of(1L, 2L), true))
			.thenReturn(List.of(answer1, answer2));

		// when
		ParticipantCorrectAnswerRankDto result = questionRankService.getParticipantCorrectRank(questionSetId);

		// then
		assertNotNull(result);
		assertThat(result.getActiveParticipants()).hasSize(2);
		assertThat(result.getActiveParticipants().get(0).getCorrectAnswerCount()).isEqualTo(1L);
		assertThat(result.getActiveParticipants().get(1).getCorrectAnswerCount()).isEqualTo(1L);
		assertThat(result.getEliminatedParticipants()).isEmpty();

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionSetParticipantRepository).findAllByQuestionSetWithFetchJoinUser(questionSet);
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(answerSubmitRecordEntityRepository).findAllByQuestionIdInAndIsCorrect(List.of(1L, 2L), true);
	}

	@Test
	@DisplayName("참가자 정답 랭킹 조회 성공 - 탈락 참가자만 있는 경우")
	void getParticipantCorrectRank_OnlyEliminatedParticipants() {
		// given
		final Long questionSetId = 1L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		UserEntity user1 = mock(UserEntity.class);

		when(user1.getId()).thenReturn(1L);

		QuestionSetParticipantEntity participant1 = mock(QuestionSetParticipantEntity.class);

		when(participant1.getId()).thenReturn(1L);
		when(participant1.getUser()).thenReturn(user1);
		when(participant1.getParticipantName()).thenReturn("탈락자1");
		when(participant1.getStatus()).thenReturn(ParticipantStatus.ELIMINATED);

		QuestionEntity question1 = mock(QuestionEntity.class);
		when(question1.getId()).thenReturn(1L);

		AnswerSubmitRecordEntity answer1 = mock(AnswerSubmitRecordEntity.class);
		when(answer1.getUserId()).thenReturn(1L);
		when(answer1.getQuestionId()).thenReturn(1L);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSetParticipantRepository.findAllByQuestionSetWithFetchJoinUser(questionSet))
			.thenReturn(List.of(participant1));
		when(questionEntityRepository.findAllByQuestionSetId(questionSetId))
			.thenReturn(List.of(question1));
		when(answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(List.of(1L), true))
			.thenReturn(List.of(answer1));

		// when
		ParticipantCorrectAnswerRankDto result = questionRankService.getParticipantCorrectRank(questionSetId);

		// then
		assertNotNull(result);
		assertThat(result.getActiveParticipants()).isEmpty();
		assertThat(result.getEliminatedParticipants()).hasSize(1);
		assertThat(result.getEliminatedParticipants().get(0).getCorrectAnswerCount()).isEqualTo(1L);

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionSetParticipantRepository).findAllByQuestionSetWithFetchJoinUser(questionSet);
		verify(questionEntityRepository).findAllByQuestionSetId(questionSetId);
		verify(answerSubmitRecordEntityRepository).findAllByQuestionIdInAndIsCorrect(List.of(1L), true);
	}

	@Test
	@DisplayName("정답자 랭킹 조회 성공 - 정답 개수별 그룹화 및 오름차순 정렬")
	void getCorrectorsByQuestionSetId_Success() {
		// given
		final Long questionSetId = 1L;
		final Long teamId = 10L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		TeamEntity team = mock(TeamEntity.class);
		QuestionEntity question1 = mock(QuestionEntity.class);
		QuestionEntity question2 = mock(QuestionEntity.class);
		QuestionEntity question3 = mock(QuestionEntity.class);

		UserEntity user1 = mock(UserEntity.class);
		UserEntity user2 = mock(UserEntity.class);
		UserEntity user3 = mock(UserEntity.class);

		when(questionSet.getTeamId()).thenReturn(teamId);
		when(question1.getId()).thenReturn(1L);
		when(question2.getId()).thenReturn(2L);
		when(question3.getId()).thenReturn(3L);

		when(user1.getId()).thenReturn(1L);
		when(user1.getEmail()).thenReturn("user1@test.com");
		when(user1.getName()).thenReturn("사용자1");
		when(user1.getNickname()).thenReturn("닉네임1");
		when(user1.getNicknameCode()).thenReturn("001");
		when(user1.getFullNickname()).thenReturn("닉네임1#001");

		when(user2.getId()).thenReturn(2L);
		when(user2.getEmail()).thenReturn("user2@test.com");
		when(user2.getName()).thenReturn("사용자2");
		when(user2.getNickname()).thenReturn("닉네임2");
		when(user2.getNicknameCode()).thenReturn("002");
		when(user2.getFullNickname()).thenReturn("닉네임2#002");

		when(user3.getId()).thenReturn(3L);
		when(user3.getEmail()).thenReturn("user3@test.com");
		when(user3.getName()).thenReturn("사용자3");
		when(user3.getNickname()).thenReturn("닉네임3");
		when(user3.getNicknameCode()).thenReturn("003");
		when(user3.getFullNickname()).thenReturn("닉네임3#003");

		// user1: 3개 정답, user2: 1개 정답, user3: 2개 정답
		AnswerSubmitRecordEntity answer1 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer2 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer3 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer4 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer5 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer6 = mock(AnswerSubmitRecordEntity.class);

		when(answer1.getUserId()).thenReturn(1L);
		when(answer2.getUserId()).thenReturn(1L);
		when(answer3.getUserId()).thenReturn(1L);
		when(answer4.getUserId()).thenReturn(2L);
		when(answer5.getUserId()).thenReturn(3L);
		when(answer6.getUserId()).thenReturn(3L);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionReader.getQuestionsByQuestionSet(questionSet))
			.thenReturn(List.of(question1, question2, question3));
		when(answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(
			List.of(1L, 2L, 3L), true))
			.thenReturn(List.of(answer1, answer2, answer3, answer4, answer5, answer6));
		when(teamReader.getTeam(teamId)).thenReturn(team);
		when(userReader.getUsersByTeam(team)).thenReturn(List.of(user1, user2, user3));

		// when
		List<AnswerRankDto> result = questionRankService.getCorrectorsByQuestionSetId(questionSetId);

		// then
		assertNotNull(result);
		assertThat(result).hasSize(3);

		// 정답 개수 기준 오름차순 정렬 확인 (1개, 2개, 3개)
		assertThat(result.get(0).getAnswerCount()).isEqualTo(1L);
		assertThat(result.get(0).getUsers()).hasSize(1);
		assertThat(result.get(0).getUsers().get(0).getId()).isEqualTo(2L);

		assertThat(result.get(1).getAnswerCount()).isEqualTo(2L);
		assertThat(result.get(1).getUsers()).hasSize(1);
		assertThat(result.get(1).getUsers().get(0).getId()).isEqualTo(3L);

		assertThat(result.get(2).getAnswerCount()).isEqualTo(3L);
		assertThat(result.get(2).getUsers()).hasSize(1);
		assertThat(result.get(2).getUsers().get(0).getId()).isEqualTo(1L);

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionReader).getQuestionsByQuestionSet(questionSet);
		verify(answerSubmitRecordEntityRepository).findAllByQuestionIdInAndIsCorrect(List.of(1L, 2L, 3L), true);
		verify(teamReader).getTeam(teamId);
		verify(userReader).getUsersByTeam(team);
	}

	@Test
	@DisplayName("정답자 랭킹 조회 성공 - 정답이 없는 사용자 포함")
	void getCorrectorsByQuestionSetId_WithNoCorrectAnswers() {
		// given
		final Long questionSetId = 1L;
		final Long teamId = 10L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		TeamEntity team = mock(TeamEntity.class);
		QuestionEntity question1 = mock(QuestionEntity.class);

		UserEntity user1 = mock(UserEntity.class);
		UserEntity user2 = mock(UserEntity.class);

		when(questionSet.getTeamId()).thenReturn(teamId);
		when(question1.getId()).thenReturn(1L);

		when(user1.getId()).thenReturn(1L);
		when(user1.getEmail()).thenReturn("user1@test.com");
		when(user1.getName()).thenReturn("사용자1");
		when(user1.getNickname()).thenReturn("닉네임1");
		when(user1.getNicknameCode()).thenReturn("001");
		when(user1.getFullNickname()).thenReturn("닉네임1#001");

		when(user2.getId()).thenReturn(2L);
		when(user2.getEmail()).thenReturn("user2@test.com");
		when(user2.getName()).thenReturn("사용자2");
		when(user2.getNickname()).thenReturn("닉네임2");
		when(user2.getNicknameCode()).thenReturn("002");
		when(user2.getFullNickname()).thenReturn("닉네임2#002");

		// user1: 1개 정답, user2: 정답 없음
		AnswerSubmitRecordEntity answer1 = mock(AnswerSubmitRecordEntity.class);
		when(answer1.getUserId()).thenReturn(1L);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionReader.getQuestionsByQuestionSet(questionSet))
			.thenReturn(List.of(question1));
		when(answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(
			List.of(1L), true))
			.thenReturn(List.of(answer1));
		when(teamReader.getTeam(teamId)).thenReturn(team);
		when(userReader.getUsersByTeam(team)).thenReturn(List.of(user1, user2));

		// when
		List<AnswerRankDto> result = questionRankService.getCorrectorsByQuestionSetId(questionSetId);

		// then
		assertNotNull(result);
		assertThat(result).hasSize(2);

		// 정답 개수 기준 오름차순 정렬 확인 (0개, 1개)
		assertThat(result.get(0).getAnswerCount()).isEqualTo(0L);
		assertThat(result.get(0).getUsers()).hasSize(1);
		assertThat(result.get(0).getUsers().get(0).getId()).isEqualTo(2L);

		assertThat(result.get(1).getAnswerCount()).isEqualTo(1L);
		assertThat(result.get(1).getUsers()).hasSize(1);
		assertThat(result.get(1).getUsers().get(0).getId()).isEqualTo(1L);

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionReader).getQuestionsByQuestionSet(questionSet);
		verify(answerSubmitRecordEntityRepository).findAllByQuestionIdInAndIsCorrect(List.of(1L), true);
		verify(teamReader).getTeam(teamId);
		verify(userReader).getUsersByTeam(team);
	}

	@Test
	@DisplayName("정답자 랭킹 조회 성공 - 동일한 정답 개수를 가진 사용자들 그룹화")
	void getCorrectorsByQuestionSetId_SameAnswerCount() {
		// given
		final Long questionSetId = 1L;
		final Long teamId = 10L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		TeamEntity team = mock(TeamEntity.class);
		QuestionEntity question1 = mock(QuestionEntity.class);
		QuestionEntity question2 = mock(QuestionEntity.class);

		UserEntity user1 = mock(UserEntity.class);
		UserEntity user2 = mock(UserEntity.class);
		UserEntity user3 = mock(UserEntity.class);

		when(questionSet.getTeamId()).thenReturn(teamId);
		when(question1.getId()).thenReturn(1L);
		when(question2.getId()).thenReturn(2L);

		when(user1.getId()).thenReturn(1L);
		when(user1.getEmail()).thenReturn("user1@test.com");
		when(user1.getName()).thenReturn("사용자1");
		when(user1.getNickname()).thenReturn("닉네임1");
		when(user1.getNicknameCode()).thenReturn("001");
		when(user1.getFullNickname()).thenReturn("닉네임1#001");

		when(user2.getId()).thenReturn(2L);
		when(user2.getEmail()).thenReturn("user2@test.com");
		when(user2.getName()).thenReturn("사용자2");
		when(user2.getNickname()).thenReturn("닉네임2");
		when(user2.getNicknameCode()).thenReturn("002");
		when(user2.getFullNickname()).thenReturn("닉네임2#002");

		when(user3.getId()).thenReturn(3L);
		when(user3.getEmail()).thenReturn("user3@test.com");
		when(user3.getName()).thenReturn("사용자3");
		when(user3.getNickname()).thenReturn("닉네임3");
		when(user3.getNicknameCode()).thenReturn("003");
		when(user3.getFullNickname()).thenReturn("닉네임3#003");

		// user1, user2: 각각 1개 정답, user3: 정답 없음
		AnswerSubmitRecordEntity answer1 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity answer2 = mock(AnswerSubmitRecordEntity.class);

		when(answer1.getUserId()).thenReturn(1L);
		when(answer2.getUserId()).thenReturn(2L);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionReader.getQuestionsByQuestionSet(questionSet))
			.thenReturn(List.of(question1, question2));
		when(answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(
			List.of(1L, 2L), true))
			.thenReturn(List.of(answer1, answer2));
		when(teamReader.getTeam(teamId)).thenReturn(team);
		when(userReader.getUsersByTeam(team)).thenReturn(List.of(user1, user2, user3));

		// when
		List<AnswerRankDto> result = questionRankService.getCorrectorsByQuestionSetId(questionSetId);

		// then
		assertNotNull(result);
		assertThat(result).hasSize(2);

		// 정답 개수 기준 오름차순 정렬 확인 (0개, 1개)
		assertThat(result.get(0).getAnswerCount()).isEqualTo(0L);
		assertThat(result.get(0).getUsers()).hasSize(1);
		assertThat(result.get(0).getUsers().get(0).getId()).isEqualTo(3L);

		// 동일한 정답 개수(1개)를 가진 사용자들이 같은 그룹에 포함되는지 확인
		assertThat(result.get(1).getAnswerCount()).isEqualTo(1L);
		assertThat(result.get(1).getUsers()).hasSize(2);
		assertThat(result.get(1).getUsers()).extracting(UserDto::getId).containsExactlyInAnyOrder(1L, 2L);

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionReader).getQuestionsByQuestionSet(questionSet);
		verify(answerSubmitRecordEntityRepository).findAllByQuestionIdInAndIsCorrect(List.of(1L, 2L), true);
		verify(teamReader).getTeam(teamId);
		verify(userReader).getUsersByTeam(team);
	}

	@Test
	@DisplayName("정답자 랭킹 조회 실패 - 존재하지 않는 문제 세트")
	void getCorrectorsByQuestionSetId_QuestionSetNotFound() {
		// given
		final Long questionSetId = 999L;

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionRankService.getCorrectorsByQuestionSetId(questionSetId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("해당 문제 세트가 존재하지 않습니다.");

		verify(questionSetEntityRepository).findById(questionSetId);
		verifyNoInteractions(questionReader);
		verifyNoInteractions(answerSubmitRecordEntityRepository);
		verifyNoInteractions(teamReader);
		verifyNoInteractions(userReader);
	}

	@Test
	@DisplayName("정답자 랭킹 조회 성공 - 사용자가 없는 경우")
	void getCorrectorsByQuestionSetId_NoUsers() {
		// given
		final Long questionSetId = 1L;
		final Long teamId = 10L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		TeamEntity team = mock(TeamEntity.class);
		QuestionEntity question1 = mock(QuestionEntity.class);

		when(questionSet.getTeamId()).thenReturn(teamId);
		when(question1.getId()).thenReturn(1L);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionReader.getQuestionsByQuestionSet(questionSet))
			.thenReturn(List.of(question1));
		when(answerSubmitRecordEntityRepository.findAllByQuestionIdInAndIsCorrect(
			List.of(1L), true))
			.thenReturn(List.of());
		when(teamReader.getTeam(teamId)).thenReturn(team);
		when(userReader.getUsersByTeam(team)).thenReturn(List.of());

		// when
		List<AnswerRankDto> result = questionRankService.getCorrectorsByQuestionSetId(questionSetId);

		// then
		assertNotNull(result);
		assertThat(result).isEmpty();

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(questionReader).getQuestionsByQuestionSet(questionSet);
		verify(answerSubmitRecordEntityRepository).findAllByQuestionIdInAndIsCorrect(List.of(1L), true);
		verify(teamReader).getTeam(teamId);
		verify(userReader).getUsersByTeam(team);
	}
}
