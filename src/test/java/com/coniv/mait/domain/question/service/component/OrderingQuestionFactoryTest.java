package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;

@ExtendWith(MockitoExtension.class)
class OrderingQuestionFactoryTest {

	@InjectMocks
	private OrderingQuestionFactory orderingQuestionFactory;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Mock
	private QuestionSetEntity questionSetEntity;

	@Test
	@DisplayName("getQuestionType - ORDERING 타입 반환")
	void getQuestionType() {
		// when
		QuestionType result = orderingQuestionFactory.getQuestionType();

		// then
		assertEquals(QuestionType.ORDERING, result);
	}

	@Test
	@DisplayName("save - 순서배열 문제와 옵션 저장 성공")
	void save_Success() {
		// given
		List<OrderingQuestionOptionDto> options = List.of(
			OrderingQuestionOptionDto.builder().content("옵션 1").originOrder(1).answerOrder(2).build(),
			OrderingQuestionOptionDto.builder().content("옵션 2").originOrder(2).answerOrder(1).build(),
			OrderingQuestionOptionDto.builder().content("옵션 3").originOrder(3).answerOrder(3).build()
		);

		OrderingQuestionDto questionDto = OrderingQuestionDto.builder()
			.content("순서배열 문제 내용")
			.explanation("해설")
			.number(1L)
			.options(options)
			.build();

		// when
		QuestionEntity result = orderingQuestionFactory.save(questionDto, questionSetEntity);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(OrderingQuestionEntity.class);
		verify(questionEntityRepository).save(any(OrderingQuestionEntity.class));
		verify(orderingOptionEntityRepository).saveAll(any());
	}

	@Test
	@DisplayName("getQuestion - answerVisible=true일 때 정답 포함하여 반환")
	void getQuestion_AnswerVisible() {
		// given
		OrderingQuestionEntity question = mock(OrderingQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<OrderingOptionEntity> options = List.of(
			mock(OrderingOptionEntity.class),
			mock(OrderingOptionEntity.class)
		);
		when(orderingOptionEntityRepository.findAllByOrderingQuestionId(1L)).thenReturn(options);

		// when
		QuestionDto result = orderingQuestionFactory.getQuestion(question, true);

		// then
		assertThat(result).isInstanceOf(OrderingQuestionDto.class);
		verify(orderingOptionEntityRepository).findAllByOrderingQuestionId(1L);
	}

	@Test
	@DisplayName("getQuestion - answerVisible=false일 때 정답 숨김하여 반환")
	void getQuestion_AnswerHidden() {
		// given
		OrderingQuestionEntity question = mock(OrderingQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		List<OrderingOptionEntity> options = List.of(
			mock(OrderingOptionEntity.class),
			mock(OrderingOptionEntity.class)
		);
		when(orderingOptionEntityRepository.findAllByOrderingQuestionId(1L)).thenReturn(options);

		// when
		QuestionDto result = orderingQuestionFactory.getQuestion(question, false);

		// then
		assertThat(result).isInstanceOf(OrderingQuestionDto.class);
		verify(orderingOptionEntityRepository).findAllByOrderingQuestionId(1L);
	}

	@Test
	@DisplayName("createOrderingQuestionOptions - 중복된 정답 순서 시 예외 발생")
	void createOrderingQuestionOptions_DuplicateAnswerOrder_ThrowsException() {
		// given
		List<OrderingQuestionOptionDto> options = List.of(
			OrderingQuestionOptionDto.builder().content("옵션 1").originOrder(1).answerOrder(1).build(),
			OrderingQuestionOptionDto.builder().content("옵션 2").originOrder(2).answerOrder(1).build() // 중복 정답 순서
		);

		OrderingQuestionEntity question = mock(OrderingQuestionEntity.class);
		// Mock 설정 제거 - 예외가 먼저 발생해서 getId() 호출되지 않음

		// when & then
		assertThrows(UserParameterException.class,
			() -> orderingQuestionFactory.createOrderingQuestionOptions(options, question));
	}

	@Test
	@DisplayName("createOrderingQuestionOptions - 중복된 원본 순서 시 예외 발생")
	void createOrderingQuestionOptions_DuplicateOriginOrder_ThrowsException() {
		// given
		List<OrderingQuestionOptionDto> options = List.of(
			OrderingQuestionOptionDto.builder().content("옵션 1").originOrder(1).answerOrder(1).build(),
			OrderingQuestionOptionDto.builder().content("옵션 2").originOrder(1).answerOrder(2).build() // 중복 원본 순서
		);

		OrderingQuestionEntity question = mock(OrderingQuestionEntity.class);

		// when & then
		assertThrows(UserParameterException.class,
			() -> orderingQuestionFactory.createOrderingQuestionOptions(options, question));
	}

	@Test
	@DisplayName("createOrderingQuestionOptions - 정상적인 옵션 생성")
	void createOrderingQuestionOptions_Success() {
		// given
		List<OrderingQuestionOptionDto> options = List.of(
			OrderingQuestionOptionDto.builder().content("옵션 1").originOrder(1).answerOrder(2).build(),
			OrderingQuestionOptionDto.builder().content("옵션 2").originOrder(2).answerOrder(1).build()
		);

		OrderingQuestionEntity question = mock(OrderingQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		// when
		List<OrderingOptionEntity> result = orderingQuestionFactory.createOrderingQuestionOptions(options, question);

		// then
		assertEquals(2, result.size());
	}

	@Test
	@DisplayName("deleteSubEntities - 하위 엔티티들을 삭제한다")
	void deleteSubEntities() {
		// given
		OrderingQuestionEntity question = mock(OrderingQuestionEntity.class);
		when(question.getId()).thenReturn(1L);

		// when
		orderingQuestionFactory.deleteSubEntities(question);

		// then
		verify(orderingOptionEntityRepository).deleteAllByOrderingQuestionId(1L);
	}

	@Test
	@DisplayName("createSubEntities - 하위 엔티티들을 생성하여 저장한다")
	void createSubEntities() {
		// given
		List<OrderingQuestionOptionDto> options = List.of(
			OrderingQuestionOptionDto.builder().content("옵션 1").originOrder(1).answerOrder(2).build(),
			OrderingQuestionOptionDto.builder().content("옵션 2").originOrder(2).answerOrder(1).build()
		);

		OrderingQuestionDto questionDto = OrderingQuestionDto.builder()
			.content("순서배열 문제")
			.options(options)
			.build();

		OrderingQuestionEntity question = mock(OrderingQuestionEntity.class);

		// when
		orderingQuestionFactory.createSubEntities(questionDto, question);

		// then
		verify(orderingOptionEntityRepository).saveAll(any(List.class));
	}
}
