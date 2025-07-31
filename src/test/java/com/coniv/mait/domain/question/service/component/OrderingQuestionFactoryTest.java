package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

@ExtendWith(MockitoExtension.class)
class OrderingQuestionFactoryTest {

	@InjectMocks
	private OrderingQuestionFactory orderingQuestionFactory;

	@Test
	@DisplayName("순서배열 문제 생성 테스트 - 정상적으로 생성")
	void create_Success() {
		// given
		List<OrderingQuestionOptionDto> options = createValidOptions();

		OrderingQuestionDto questionDto = OrderingQuestionDto.builder()
			.content("순서배열 문제 내용")
			.explanation("문제 해설")
			.number(1L)
			.options(options)
			.build();

		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 세트", QuestionSetCreationType.MANUAL);
		int expectedDisplayDelay = 2500;

		// when
		try (MockedStatic<RandomUtil> mockedRandomUtil = mockStatic(RandomUtil.class)) {
			mockedRandomUtil.when(() -> RandomUtil.getRandomNumber(5000))
				.thenReturn(expectedDisplayDelay);

			OrderingQuestionEntity result = orderingQuestionFactory.create(questionDto, questionSet);

			// then
			assertThat(result).isNotNull();
			assertThat(result.getId()).isNull();
			assertThat(result.getContent()).isEqualTo("순서배열 문제 내용");
			assertThat(result.getExplanation()).isEqualTo("문제 해설");
			assertThat(result.getNumber()).isEqualTo(1L);
			assertThat(result.getDisplayDelayMilliseconds()).isEqualTo(expectedDisplayDelay);
			assertThat(result.getQuestionSet()).isEqualTo(questionSet);

			mockedRandomUtil.verify(() -> RandomUtil.getRandomNumber(5000));
		}
	}

	@Test
	@DisplayName("순서배열 선택지 리스트 생성 테스트 - 정상적으로 생성")
	void createOrderingQuestionOptions_Success() {
		// given
		OrderingQuestionEntity question = OrderingQuestionEntity.builder()
			.content("순서배열 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(null)
			.id(10L)
			.build();

		List<OrderingQuestionOptionDto> optionDtos = createValidOptions();

		// when
		List<OrderingOptionEntity> result = orderingQuestionFactory.createOrderingQuestionOptions(optionDtos, question);

		// then
		assertThat(result).hasSize(3);

		OrderingOptionEntity first = result.getFirst();
		assertThat(first.getId()).isNull();
		assertThat(first.getContent()).isEqualTo("첫 번째 옵션");
		assertThat(first.getOriginOrder()).isEqualTo(1);
		assertThat(first.getAnswerOrder()).isEqualTo(2);
		assertThat(first.getOrderingQuestionId()).isEqualTo(10L);

		OrderingOptionEntity second = result.get(1);
		assertThat(second.getId()).isNull();
		assertThat(second.getContent()).isEqualTo("두 번째 옵션");
		assertThat(second.getOriginOrder()).isEqualTo(2);
		assertThat(second.getAnswerOrder()).isEqualTo(1);
		assertThat(second.getOrderingQuestionId()).isEqualTo(10L);

		OrderingOptionEntity third = result.get(2);
		assertThat(third.getId()).isNull();
		assertThat(third.getContent()).isEqualTo("세 번째 옵션");
		assertThat(third.getOriginOrder()).isEqualTo(3);
		assertThat(third.getAnswerOrder()).isEqualTo(3);
		assertThat(third.getOrderingQuestionId()).isEqualTo(10L);
	}

	@Test
	@DisplayName("순서배열 선택지 생성 실패 - 중복된 정답 순서(answerOrder)")
	void createOrderingQuestionOptions_DuplicateAnswerOrder_ThrowsException() {
		// given
		OrderingQuestionEntity question = OrderingQuestionEntity.builder()
			.content("순서배열 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(null)
			.id(10L)
			.build();

		List<OrderingQuestionOptionDto> duplicateAnswerOrderOptions = Arrays.asList(
			OrderingQuestionOptionDto.builder()
				.content("첫 번째 옵션")
				.originOrder(1)
				.answerOrder(1) // 중복
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("두 번째 옵션")
				.originOrder(2)
				.answerOrder(1) // 중복
				.build()
		);

		// when & then
		assertThatThrownBy(
			() -> orderingQuestionFactory.createOrderingQuestionOptions(duplicateAnswerOrderOptions, question))
			.isInstanceOf(UserParameterException.class)
			.hasMessageContaining("Ordering question options must have unique answer orders.");
	}

	@Test
	@DisplayName("순서배열 선택지 생성 실패 - 중복된 원본 순서(originOrder)")
	void createOrderingQuestionOptions_DuplicateOriginOrder_ThrowsException() {
		// given
		OrderingQuestionEntity question = OrderingQuestionEntity.builder()
			.content("순서배열 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(null)
			.id(10L)
			.build();

		List<OrderingQuestionOptionDto> duplicateOriginOrderOptions = Arrays.asList(
			OrderingQuestionOptionDto.builder()
				.content("첫 번째 옵션")
				.originOrder(1) // 중복
				.answerOrder(1)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("두 번째 옵션")
				.originOrder(1) // 중복
				.answerOrder(2)
				.build()
		);

		// when & then
		assertThatThrownBy(
			() -> orderingQuestionFactory.createOrderingQuestionOptions(duplicateOriginOrderOptions, question))
			.isInstanceOf(UserParameterException.class)
			.hasMessageContaining("Ordering question options must have unique origin orders.");
	}

	@Test
	@DisplayName("순서배열 선택지 생성 테스트 - 빈 리스트")
	void createOrderingQuestionOptions_EmptyList_Success() {
		// given
		OrderingQuestionEntity question = OrderingQuestionEntity.builder()
			.content("순서배열 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(null)
			.id(10L)
			.build();

		List<OrderingQuestionOptionDto> emptyOptions = List.of();

		// when
		List<OrderingOptionEntity> result = orderingQuestionFactory.createOrderingQuestionOptions(emptyOptions,
			question);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("순서배열 선택지 생성 테스트 - 단일 선택지")
	void createOrderingQuestionOptions_SingleOption_Success() {
		// given
		OrderingQuestionEntity question = OrderingQuestionEntity.builder()
			.content("순서배열 문제")
			.explanation("설명")
			.number(1L)
			.displayDelayMilliseconds(1000)
			.questionSet(null)
			.id(10L)
			.build();

		List<OrderingQuestionOptionDto> singleOption = Collections.singletonList(
			OrderingQuestionOptionDto.builder()
				.content("유일한 옵션")
				.originOrder(1)
				.answerOrder(1)
				.build()
		);

		// when
		List<OrderingOptionEntity> result = orderingQuestionFactory.createOrderingQuestionOptions(singleOption,
			question);

		// then
		assertThat(result).hasSize(1);
		OrderingOptionEntity option = result.getFirst();
		assertThat(option.getContent()).isEqualTo("유일한 옵션");
		assertThat(option.getOriginOrder()).isEqualTo(1);
		assertThat(option.getAnswerOrder()).isEqualTo(1);
		assertThat(option.getOrderingQuestionId()).isEqualTo(10L);
	}

	private List<OrderingQuestionOptionDto> createValidOptions() {
		return Arrays.asList(
			OrderingQuestionOptionDto.builder()
				.content("첫 번째 옵션")
				.originOrder(1)
				.answerOrder(2)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("두 번째 옵션")
				.originOrder(2)
				.answerOrder(1)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("세 번째 옵션")
				.originOrder(3)
				.answerOrder(3)
				.build()
		);
	}
}
