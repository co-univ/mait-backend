package com.coniv.mait.domain.solve.service.component;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.OrderingOptionRepository;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

@ExtendWith(MockitoExtension.class)
class OrderingQuestionAnswerCheckerTest {

	@InjectMocks
	private OrderingQuestionAnswerChecker orderingQuestionAnswerChecker;

	@Mock
	private OrderingOptionRepository orderingOptionRepository;

	@Test
	@DisplayName("정답: 정해진 순서로 originOrder 제출")
	void checkAnswer_correctOrder() {
		OrderingOptionRepository repo = mock(OrderingOptionRepository.class);
		OrderingQuestionAnswerChecker checker = new OrderingQuestionAnswerChecker(repo);

		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<OrderingOptionEntity> optionEntities = List.of(
			OrderingOptionEntity.builder()
				.id(1L)
				.originOrder(1)
				.content("신유승")
				.answerOrder(1)
				.orderingQuestionId(1L)
				.build(),
			OrderingOptionEntity.builder()
				.id(2L)
				.originOrder(2)
				.content("손민재")
				.answerOrder(3)
				.orderingQuestionId(1L)
				.build(),
			OrderingOptionEntity.builder()
				.id(3L)
				.originOrder(3)
				.content("이하은")
				.answerOrder(4)
				.orderingQuestionId(1L)
				.build(),
			OrderingOptionEntity.builder()
				.id(4L)
				.originOrder(4)
				.content("전민재")
				.answerOrder(2)
				.orderingQuestionId(1L)
				.build()
		);
		when(repo.findAllByOrderingQuestionId(1L)).thenReturn(optionEntities);

		// 정답: [1, 4, 2, 3]
		SubmitAnswerDto<Long> submit = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.ORDERING;
			}

			@Override
			public List<Long> getSubmitAnswers() {
				return List.of(1L, 4L, 2L, 3L);
			}
		};

		assertThat(checker.checkAnswer(question, submit)).isTrue();
	}

	@Test
	@DisplayName("오답: 순서가 틀림")
	void checkAnswer_wrongOrder() {
		OrderingOptionRepository repo = mock(OrderingOptionRepository.class);
		OrderingQuestionAnswerChecker checker = new OrderingQuestionAnswerChecker(repo);

		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<OrderingOptionEntity> optionEntities = List.of(
			OrderingOptionEntity.builder()
				.id(1L)
				.originOrder(1)
				.content("신유승")
				.answerOrder(1)
				.orderingQuestionId(1L)
				.build(),
			OrderingOptionEntity.builder()
				.id(2L)
				.originOrder(2)
				.content("손민재")
				.answerOrder(3)
				.orderingQuestionId(1L)
				.build(),
			OrderingOptionEntity.builder()
				.id(3L)
				.originOrder(3)
				.content("이하은")
				.answerOrder(4)
				.orderingQuestionId(1L)
				.build(),
			OrderingOptionEntity.builder()
				.id(4L)
				.originOrder(4)
				.content("전민재")
				.answerOrder(2)
				.orderingQuestionId(1L)
				.build()
		);
		when(repo.findAllByOrderingQuestionId(1L)).thenReturn(optionEntities);

		// 오답: [4, 1, 2, 3]
		SubmitAnswerDto<Long> submit = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.ORDERING;
			}

			@Override
			public List<Long> getSubmitAnswers() {
				return List.of(4L, 1L, 2L, 3L);
			}
		};

		assertThat(checker.checkAnswer(question, submit)).isFalse();
	}
}
