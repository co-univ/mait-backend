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

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.dto.ParticipantCorrectAnswerRankDto;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;

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
}
