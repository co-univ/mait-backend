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

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.solve.service.dto.FillBlankSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

@ExtendWith(MockitoExtension.class)
class FillBlankQuestionAnswerCheckerTest {

	@InjectMocks
	private FillBlankQuestionAnswerChecker fillBlankQuestionAnswerChecker;

	@Mock
	private FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Test
	@DisplayName("빈칸 - 정답 케이스 (복수 정답 허용, 순서 일치)")
	void checkAnswer_allCorrect() {

		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		// 1번 빈칸: 신유승, 유승 / 2번 빈칸: 스끼야끼, 카이센동
		List<FillBlankAnswerEntity> entities = List.of(
			FillBlankAnswerEntity.builder()
				.id(1L)
				.answer("신유승")
				.fillBlankQuestionId(1L)
				.isMain(true)
				.number(1L)
				.build(),
			FillBlankAnswerEntity.builder()
				.id(2L)
				.answer("유승")
				.fillBlankQuestionId(1L)
				.isMain(false)
				.number(1L)
				.build(),
			FillBlankAnswerEntity.builder()
				.id(3L)
				.answer("스끼야끼")
				.fillBlankQuestionId(1L)
				.isMain(true)
				.number(2L)
				.build(),
			FillBlankAnswerEntity.builder()
				.id(4L)
				.answer("카이센동")
				.fillBlankQuestionId(1L)
				.isMain(false)
				.number(2L)
				.build()
		);
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(entities);

		// 1. 유승, 스끼야끼 -> 정답
		SubmitAnswerDto<FillBlankSubmitAnswer> submit1 = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.FILL_BLANK;
			}

			@Override
			public List<FillBlankSubmitAnswer> getSubmitAnswers() {
				return List.of(
					new FillBlankSubmitAnswer(1L, "유승"),
					new FillBlankSubmitAnswer(2L, "스끼야끼")
				);
			}
		};

		assertThat(fillBlankQuestionAnswerChecker.checkAnswer(question, submit1)).isTrue();
	}

	@Test
	@DisplayName("빈칸 - 오답 케이스 (순서 일치하지만 값 불일치)")
	void checkAnswer_wrongAnswer() {
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<FillBlankAnswerEntity> entities = List.of(
			FillBlankAnswerEntity.builder()
				.id(1L)
				.answer("신유승")
				.fillBlankQuestionId(1L)
				.isMain(true)
				.number(1L)
				.build(),
			FillBlankAnswerEntity.builder()
				.id(2L)
				.answer("유승")
				.fillBlankQuestionId(1L)
				.isMain(false)
				.number(1L)
				.build(),
			FillBlankAnswerEntity.builder()
				.id(3L)
				.answer("스끼야끼")
				.fillBlankQuestionId(1L)
				.isMain(true)
				.number(2L)
				.build(),
			FillBlankAnswerEntity.builder()
				.id(4L)
				.answer("카이센동")
				.fillBlankQuestionId(1L)
				.isMain(false)
				.number(2L)
				.build()
		);
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(entities);

		// 2. 유승, 신유승 -> 오답 (2번 빈칸 불일치)
		SubmitAnswerDto<FillBlankSubmitAnswer> submit2 = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.FILL_BLANK;
			}

			@Override
			public List<FillBlankSubmitAnswer> getSubmitAnswers() {
				return List.of(
					new FillBlankSubmitAnswer(1L, "유승"),
					new FillBlankSubmitAnswer(2L, "신유승")
				);
			}
		};

		// 3. 스끼야끼, 유승 -> 오답 (1번 빈칸 불일치)
		SubmitAnswerDto<FillBlankSubmitAnswer> submit3 = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.FILL_BLANK;
			}

			@Override
			public List<FillBlankSubmitAnswer> getSubmitAnswers() {
				return List.of(
					new FillBlankSubmitAnswer(1L, "스끼야끼"),
					new FillBlankSubmitAnswer(2L, "유승")
				);
			}
		};

		assertThat(fillBlankQuestionAnswerChecker.checkAnswer(question, submit2)).isFalse();
		assertThat(fillBlankQuestionAnswerChecker.checkAnswer(question, submit3)).isFalse();
	}
}
