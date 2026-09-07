package com.coniv.mait.migration.question;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class MultipleQuestionAnswerCountMigrationJobTest {

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private MultipleQuestionAnswerCountMigrationJob migration;

	@Test
	@DisplayName("불일치가 없으면 데이터를 갱신하지 않는다")
	void migrate_SkipsWhenConsistent() {
		// given
		MultipleQuestionEntity question = mock(MultipleQuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getAnswerCount()).thenReturn(2);
		when(questionEntityRepository.findAllByQuestionType(QuestionType.MULTIPLE)).thenReturn(List.of(question));
		when(multipleChoiceEntityRepository.countByQuestionIdAndIsCorrectTrue(1L)).thenReturn(2);

		// when
		migration.migrate();

		// then
		verify(question, never()).updateAnswerCount(anyInt());
		verify(questionEntityRepository, never()).flush();
		verifyNoInteractions(entityManager);
	}

	@Test
	@DisplayName("보정 후 불일치가 남으면 예외를 던져 완료 처리를 막는다")
	void migrate_ThrowsWhenVerificationFails() {
		// given
		MultipleQuestionEntity question = MultipleQuestionEntity.builder().id(1L).answerCount(4).build();
		when(questionEntityRepository.findAllByQuestionType(QuestionType.MULTIPLE)).thenReturn(List.of(question));
		when(multipleChoiceEntityRepository.countByQuestionIdAndIsCorrectTrue(1L)).thenReturn(1);
		doAnswer(invocation -> {
			question.updateAnswerCount(4);
			return null;
		}).when(entityManager).refresh(question);

		// when & then
		assertThatThrownBy(migration::migrate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("불일치가 남아 있습니다: 1");
	}
}
