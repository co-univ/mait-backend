package com.coniv.mait.domain.solve.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

@ExtendWith(MockitoExtension.class)
class MultipleQuestionAnswerCheckerTest {

	@Mock
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@InjectMocks
	private MultipleQuestionAnswerChecker checker;

	@Test
	@DisplayName("정답과 제출 답안이 같으면 true 반환")
	void checkAnswer_correct() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		when(multipleChoiceEntityRepository.findAllByQuestionId(1L))
			.thenReturn(List.of(
				MultipleChoiceEntity.builder().number(1).build(),
				MultipleChoiceEntity.builder().number(2).build(),
				MultipleChoiceEntity.builder().number(3).build()
			));

		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L, 2L, 3L));

		// when
		boolean result = checker.checkAnswer(question, submitAnswer);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("정답과 제출 답안이 다르면 false 반환")
	void checkAnswer_wrong() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		when(multipleChoiceEntityRepository.findAllByQuestionId(1L))
			.thenReturn(List.of(
				MultipleChoiceEntity.builder().number(1).build(),
				MultipleChoiceEntity.builder().number(2).build()
			));

		MultipleQuestionSubmitAnswer submitAnswer = new MultipleQuestionSubmitAnswer(List.of(1L, 3L));

		// when
		boolean result = checker.checkAnswer(question, submitAnswer);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("MULTIPLE 타입이 아닌 경우 예외 발생")
	void checkAnswer_invalidType() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		SubmitAnswerDto wrongTypeDto = () -> QuestionType.SHORT;

		// when & then
		assertThatThrownBy(() -> checker.checkAnswer(question, wrongTypeDto))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid question type");
	}
}
