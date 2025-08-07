package com.coniv.mait.domain.solve.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.component.ScorerGenerator;
import com.coniv.mait.domain.solve.service.component.ScorerProcessor;
import com.coniv.mait.domain.solve.service.component.SubmitOrderGenerator;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class QuestionAnswerSubmitServiceTest {

	@InjectMocks
	private QuestionAnswerSubmitService questionAnswerSubmitService;

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Mock
	private AnswerGrader answerGrader;

	@Mock
	private SubmitOrderGenerator submitOrderGenerator;

	@Mock
	private ScorerProcessor scorerProcessor;

	@Mock
	private ScorerGenerator scorerGenerator;

	@Mock
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("이미 정답을 제출한 경우 - 중복 제출 방지")
	void submitAnswer_AlreadySubmittedCorrectAnswer_ThrowsException() {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;
		Long userId = 1L;
		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L));

		UserEntity mockUser = mock(UserEntity.class);
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		MultipleQuestionEntity mockQuestion = mock(MultipleQuestionEntity.class);
		when(mockUser.getId()).thenReturn(userId);

		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
		when(questionEntityRepository.findById(questionId)).thenReturn(Optional.of(mockQuestion));
		when(mockQuestion.getQuestionSet()).thenReturn(mockQuestionSet);
		when(mockQuestionSet.getId()).thenReturn(questionSetId);

		when(answerSubmitRecordEntityRepository.existsByUserIdAndQuestionIdAndIsCorrectTrue(userId, questionId))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> questionAnswerSubmitService.submitAnswer(
			questionSetId, questionId, userId, submitAnswer))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("이미 해당 문제에 대해 정답을 제출한 기록이 있습니다.");

		verify(answerGrader, never()).gradeAnswer(any(), any());
		verify(answerSubmitRecordEntityRepository, never()).save(any());
		verify(scorerGenerator, never()).updateScorer(any(), any(), any());
	}

}
