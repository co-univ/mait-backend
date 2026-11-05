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
import com.coniv.mait.domain.solve.service.dto.ShortQuestionSubmitAnswer;
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

	@Test
	@DisplayName("하나의 제출 답안으로 여러 정답 그룹을 채울 수 없다")
	void checkAnswer_oneSubmissionCannotFillMultipleGroups() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(2L).answer("alpha").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("alpha", "wrong"));

		// when
		boolean isCorrect = checker.checkAnswer(question, submitAnswer);

		// then
		assertThat(isCorrect).isFalse();
	}

	@Test
	@DisplayName("같은 값을 두 번 제출하면 각각 다른 정답 그룹에 배정할 수 있다")
	void checkAnswer_repeatedSubmissionMatchesDifferentGroups() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(2L).answer("alpha").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("alpha", "alpha"));

		// when
		boolean isCorrect = checker.checkAnswer(question, submitAnswer);

		// then
		assertThat(isCorrect).isTrue();
	}

	@Test
	@DisplayName("인정답안이 겹치면 다른 그룹에 배정해 모든 제출 답안을 채점한다")
	void checkAnswer_reassignsOverlappingAnswer() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(10L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(10L).answer("beta").build(),
			ShortAnswerEntity.builder().number(20L).answer("alpha").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("alpha", "beta"));

		// when
		boolean isCorrect = checker.checkAnswer(question, submitAnswer);

		// then
		assertThat(isCorrect).isTrue();
	}

	@Test
	@DisplayName("인정답안이 겹쳐도 제출 순서와 관계없이 정답 처리한다")
	void checkAnswer_reversedOverlappingAnswers() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(1L).answer("beta").build(),
			ShortAnswerEntity.builder().number(2L).answer("alpha").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("beta", "alpha"));

		// when
		boolean isCorrect = checker.checkAnswer(question, submitAnswer);

		// then
		assertThat(isCorrect).isTrue();
	}

	@Test
	@DisplayName("세 정답 그룹의 인정답안이 겹쳐도 일대일 배정이 가능하면 정답 처리한다")
	void checkAnswer_reassignsAcrossMultipleGroups() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(1L).answer("beta").build(),
			ShortAnswerEntity.builder().number(2L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(2L).answer("gamma").build(),
			ShortAnswerEntity.builder().number(3L).answer("alpha").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("alpha", "beta", "gamma"));

		// when
		boolean isCorrect = checker.checkAnswer(question, submitAnswer);

		// then
		assertThat(isCorrect).isTrue();
	}

	@Test
	@DisplayName("각 제출 값이 인정답안이어도 모든 그룹에 일대일로 배정할 수 없으면 오답 처리한다")
	void checkAnswer_noOneToOneMatching() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").build(),
			ShortAnswerEntity.builder().number(1L).answer("beta").build(),
			ShortAnswerEntity.builder().number(2L).answer("gamma").build(),
			ShortAnswerEntity.builder().number(3L).answer("gamma").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of("alpha", "beta", "gamma"));

		// when
		boolean isCorrect = checker.checkAnswer(question, submitAnswer);

		// then
		assertThat(isCorrect).isFalse();
	}

	@Test
	@DisplayName("앞뒤 공백과 대소문자를 정규화한 뒤 일대일 배정한다")
	void checkAnswer_normalizesOverlappingAnswers() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer(" ALPHA ").build(),
			ShortAnswerEntity.builder().number(1L).answer(" Beta ").build(),
			ShortAnswerEntity.builder().number(2L).answer(" alpha ").build()
		));
		ShortQuestionSubmitAnswer submitAnswer = new ShortQuestionSubmitAnswer(List.of(" alpha ", " BETA "));

		// when
		boolean isCorrect = checker.checkAnswer(question, submitAnswer);

		// then
		assertThat(isCorrect).isTrue();
	}
}
