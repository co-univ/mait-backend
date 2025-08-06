package com.coniv.mait.domain.solve.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.service.dto.QuestionScorerDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionScorerServiceTest {

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private QuestionScorerEntityRepository questionScorerEntityRepository;

	@InjectMocks
	private QuestionScorerService questionScorerService;

	@Test
	@DisplayName("득점자 조회 성공 테스트")
	void getScorer_Success() {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long userId = 1L;
		Long submitOrder = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.id(questionSetId)
			.teamId(1L)
			.build();

		QuestionEntity question = MultipleQuestionEntity.builder()
			.id(questionId)
			.number(1L)
			.questionSet(questionSet)
			.build();

		UserEntity user = mock(UserEntity.class);
		when(user.getId()).thenReturn(userId);
		when(user.getName()).thenReturn("테스트사용자");

		QuestionScorerEntity scorer = QuestionScorerEntity.builder()
			.id(1L)
			.questionId(questionId)
			.userId(userId)
			.submitOrder(submitOrder)
			.build();

		when(questionEntityRepository.findById(questionId))
			.thenReturn(Optional.of(question));
		when(questionScorerEntityRepository.findByQuestionId(questionId))
			.thenReturn(Optional.of(scorer));
		when(userEntityRepository.findById(userId))
			.thenReturn(Optional.of(user));

		// When
		QuestionScorerDto result = questionScorerService.getScorer(questionSetId, questionId);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getQuestionId()).isEqualTo(questionId);
		assertThat(result.getUserId()).isEqualTo(userId);
		assertThat(result.getUserName()).isEqualTo("테스트사용자");
		assertThat(result.getSubmitOrder()).isEqualTo(submitOrder);

		verify(questionEntityRepository).findById(questionId);
		verify(questionScorerEntityRepository).findByQuestionId(questionId);
		verify(userEntityRepository).findById(userId);
	}

	@Test
	@DisplayName("존재하지 않는 문제 ID로 조회 시 예외 발생")
	void getScorer_QuestionNotFound_ThrowsException() {
		// Given
		Long questionSetId = 1L;
		Long questionId = 999L;

		when(questionEntityRepository.findById(questionId))
			.thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> questionScorerService.getScorer(questionSetId, questionId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("문제 ID에 해당하는 문제가 없습니다.");

		verify(questionEntityRepository).findById(questionId);
		verify(questionScorerEntityRepository, never()).findByQuestionId(any());
		verify(userEntityRepository, never()).findById(any());
	}

	@Test
	@DisplayName("문제 세트 ID와 문제 ID가 일치하지 않는 경우 예외 발생")
	void getScorer_ResourceNotBelong_ThrowsException() {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long wrongQuestionSetId = 999L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.id(wrongQuestionSetId)
			.teamId(1L)
			.build();

		QuestionEntity question = MultipleQuestionEntity.builder()
			.id(questionId)
			.number(1L)
			.questionSet(questionSet)
			.build();

		when(questionEntityRepository.findById(questionId))
			.thenReturn(Optional.of(question));

		// When & Then
		assertThatThrownBy(() -> questionScorerService.getScorer(questionSetId, questionId))
			.isInstanceOf(ResourceNotBelongException.class)
			.hasMessage("문제 세트 ID와 문제 ID가 일치하지 않습니다.");

		verify(questionEntityRepository).findById(questionId);
		verify(questionScorerEntityRepository, never()).findByQuestionId(any());
		verify(userEntityRepository, never()).findById(any());
	}

	@Test
	@DisplayName("해당 문제에 득점자가 없는 경우 예외 발생")
	void getScorer_ScorerNotFound_ThrowsException() {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.id(questionSetId)
			.teamId(1L)
			.build();

		QuestionEntity question = MultipleQuestionEntity.builder()
			.id(questionId)
			.number(1L)
			.questionSet(questionSet)
			.build();

		when(questionEntityRepository.findById(questionId))
			.thenReturn(Optional.of(question));
		when(questionScorerEntityRepository.findByQuestionId(questionId))
			.thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> questionScorerService.getScorer(questionSetId, questionId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("해당 문제에 대한 득점자가 없습니다.");

		verify(questionEntityRepository).findById(questionId);
		verify(questionScorerEntityRepository).findByQuestionId(questionId);
		verify(userEntityRepository, never()).findById(any());
	}

	@Test
	@DisplayName("득점자에 해당하는 사용자가 없는 경우 예외 발생")
	void getScorer_UserNotFound_ThrowsException() {
		// Given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long userId = 999L;

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.id(questionSetId)
			.teamId(1L)
			.build();

		QuestionEntity question = MultipleQuestionEntity.builder()
			.id(questionId)
			.number(1L)
			.questionSet(questionSet)
			.build();

		QuestionScorerEntity scorer = QuestionScorerEntity.builder()
			.id(1L)
			.questionId(questionId)
			.userId(userId)
			.submitOrder(1L)
			.build();

		when(questionEntityRepository.findById(questionId))
			.thenReturn(Optional.of(question));
		when(questionScorerEntityRepository.findByQuestionId(questionId))
			.thenReturn(Optional.of(scorer));
		when(userEntityRepository.findById(userId))
			.thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> questionScorerService.getScorer(questionSetId, questionId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("득점자에 해당하는 사용자가 없습니다.");

		verify(questionEntityRepository).findById(questionId);
		verify(questionScorerEntityRepository).findByQuestionId(questionId);
		verify(userEntityRepository).findById(userId);
	}
}
