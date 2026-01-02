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

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

@ExtendWith(MockitoExtension.class)
class ShortQuestionAnswerCheckerTest {

	@Mock
	private ShortAnswerEntityRepository shortAnswerEntityRepository;

	@InjectMocks
	private ShortQuestionAnswerChecker checker;

	@Test
	@DisplayName("정답 케이스 - 순서 무관하게 전부 포함")
	void checkAnswer_allCorrect() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		// 그룹1: 연결성, 신뢰성 / 그룹2: 비연결성, 비신뢰성
		List<ShortAnswerEntity> answerEntities = List.of(
			ShortAnswerEntity.builder()
				.id(1L)
				.answer("연결성")
				.shortQuestionId(1L)
				.isMain(true)
				.number(1L)
				.build(),
			ShortAnswerEntity.builder()
				.id(2L)
				.answer("신뢰성")
				.shortQuestionId(1L)
				.isMain(false)
				.number(1L)
				.build(),
			ShortAnswerEntity.builder()
				.id(3L)
				.answer("비연결성")
				.shortQuestionId(1L)
				.isMain(true)
				.number(2L)
				.build(),
			ShortAnswerEntity.builder()
				.id(4L)
				.answer("비신뢰성")
				.shortQuestionId(1L)
				.isMain(false)
				.number(2L)
				.build()
		);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(answerEntities);

		// 1. 비신뢰성, 신뢰성 → 정답
		SubmitAnswerDto<String> submit1 = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.SHORT;
			}

			@Override
			public List<String> getSubmitAnswers() {
				return List.of("비신뢰성", "신뢰성");
			}
		};

		// 2. 비연결성, 연결성 → 정답
		SubmitAnswerDto<String> submit2 = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.SHORT;
			}

			@Override
			public List<String> getSubmitAnswers() {
				return List.of("비연결성", "연결성");
			}
		};

		// when & then
		assertThat(checker.checkAnswer(question, submit1)).isTrue();
		assertThat(checker.checkAnswer(question, submit2)).isTrue();
	}

	@Test
	@DisplayName("오답 케이스 - 중복 답변, 잘못된 답")
	void checkAnswer_someIncorrect() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<ShortAnswerEntity> answerEntities = List.of(
			ShortAnswerEntity.builder()
				.id(1L)
				.answer("연결성")
				.shortQuestionId(1L)
				.isMain(true)
				.number(1L)
				.build(),
			ShortAnswerEntity.builder()
				.id(2L)
				.answer("신뢰성")
				.shortQuestionId(1L)
				.isMain(false)
				.number(1L)
				.build(),
			ShortAnswerEntity.builder()
				.id(3L)
				.answer("비연결성")
				.shortQuestionId(1L)
				.isMain(true)
				.number(2L)
				.build(),
			ShortAnswerEntity.builder()
				.id(4L)
				.answer("비신뢰성")
				.shortQuestionId(1L)
				.isMain(false)
				.number(2L)
				.build()
		);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(answerEntities);

		// 1. 연결성, 연결성 → 오답 (그룹2 미일치)
		SubmitAnswerDto<String> submit1 = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.SHORT;
			}

			@Override
			public List<String> getSubmitAnswers() {
				return List.of("연결성", "연결성");
			}
		};

		// 2. 신뢰성, 네트워크 → 오답 (네트워크 불일치)
		SubmitAnswerDto<String> submit2 = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.SHORT;
			}

			@Override
			public List<String> getSubmitAnswers() {
				return List.of("신뢰성", "네트워크");
			}
		};

		assertThat(checker.checkAnswer(question, submit1)).isFalse();
		assertThat(checker.checkAnswer(question, submit2)).isFalse();
	}

	@Test
	@DisplayName("제출 답변 개수 불일치시 예외")
	void checkAnswer_invalidSize() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<ShortAnswerEntity> answerEntities = List.of(
			ShortAnswerEntity.builder()
				.id(1L)
				.answer("연결성")
				.shortQuestionId(1L)
				.isMain(true)
				.number(1L)
				.build(),
			ShortAnswerEntity.builder()
				.id(2L)
				.answer("신뢰성")
				.shortQuestionId(1L)
				.isMain(false)
				.number(1L)
				.build(),
			ShortAnswerEntity.builder()
				.id(3L)
				.answer("비연결성")
				.shortQuestionId(1L)
				.isMain(true)
				.number(2L)
				.build(),
			ShortAnswerEntity.builder()
				.id(4L)
				.answer("비신뢰성")
				.shortQuestionId(1L)
				.isMain(false)
				.number(2L)
				.build()
		);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(answerEntities);

		// 답 1개만 제출
		SubmitAnswerDto<String> submit = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.SHORT;
			}

			@Override
			public List<String> getSubmitAnswers() {
				return List.of("연결성");
			}
		};

		// when & then
		assertThatThrownBy(() -> checker.checkAnswer(question, submit))
			.isInstanceOf(QuestionSolvingException.class)
			.hasMessageContaining("개수");
	}
}
