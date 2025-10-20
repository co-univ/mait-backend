package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
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
import com.coniv.mait.domain.question.exception.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

@ExtendWith(MockitoExtension.class)
class OrderingQuestionValidatorTest {

	@InjectMocks
	private OrderingQuestionValidator orderingQuestionValidator;

	@Mock
	private OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Test
	@DisplayName("getQuestionType - ORDERING 타입 반환")
	void getQuestionType() {
		// when
		QuestionType result = orderingQuestionValidator.getQuestionType();

		// then
		assertThat(result).isEqualTo(QuestionType.ORDERING);
	}

	@Test
	@DisplayName("validate - 유효한 순서 문제 검증 성공")
	void validate_ValidQuestion_ReturnsValid() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("순서 문제 내용");

		OrderingOptionEntity option1 = mock(OrderingOptionEntity.class);
		when(option1.getContent()).thenReturn("옵션 1");
		OrderingOptionEntity option2 = mock(OrderingOptionEntity.class);
		when(option2.getContent()).thenReturn("옵션 2");
		OrderingOptionEntity option3 = mock(OrderingOptionEntity.class);
		when(option3.getContent()).thenReturn("옵션 3");

		List<OrderingOptionEntity> options = List.of(option1, option2, option3);
		when(orderingOptionEntityRepository.findAllByOrderingQuestionId(1L)).thenReturn(options);

		// when
		QuestionValidateDto result = orderingQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isTrue();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isNull();
		verify(orderingOptionEntityRepository).findAllByOrderingQuestionId(1L);
	}

	@Test
	@DisplayName("validate - 문제 내용이 null이면 EMPTY_CONTENT 반환")
	void validate_NullContent_ReturnsEmptyContent() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn(null);

		// when
		QuestionValidateDto result = orderingQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_CONTENT);
		verify(orderingOptionEntityRepository, never()).findAllByOrderingQuestionId(anyLong());
	}

	@Test
	@DisplayName("validate - 문제 내용이 빈 문자열이면 EMPTY_CONTENT 반환")
	void validate_BlankContent_ReturnsEmptyContent() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("   ");

		// when
		QuestionValidateDto result = orderingQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_CONTENT);
		verify(orderingOptionEntityRepository, never()).findAllByOrderingQuestionId(anyLong());
	}

	@Test
	@DisplayName("validate - 옵션이 1개만 있으면 INVALID_OPTION_COUNT 반환")
	void validate_OneOption_ReturnsInvalidOptionCount() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("순서 문제 내용");

		OrderingOptionEntity option = mock(OrderingOptionEntity.class);
		List<OrderingOptionEntity> options = List.of(option);
		when(orderingOptionEntityRepository.findAllByOrderingQuestionId(1L)).thenReturn(options);

		// when
		QuestionValidateDto result = orderingQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.INVALID_OPTION_COUNT);
		verify(orderingOptionEntityRepository).findAllByOrderingQuestionId(1L);
	}

	@Test
	@DisplayName("validate - 옵션이 없으면 INVALID_OPTION_COUNT 반환")
	void validate_NoOptions_ReturnsInvalidOptionCount() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("순서 문제 내용");

		when(orderingOptionEntityRepository.findAllByOrderingQuestionId(1L)).thenReturn(List.of());

		// when
		QuestionValidateDto result = orderingQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.INVALID_OPTION_COUNT);
		verify(orderingOptionEntityRepository).findAllByOrderingQuestionId(1L);
	}

	@Test
	@DisplayName("validate - 옵션이 정확히 2개이면 유효함")
	void validate_TwoOptions_ReturnsValid() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("순서 문제 내용");

		OrderingOptionEntity option1 = mock(OrderingOptionEntity.class);
		when(option1.getContent()).thenReturn("옵션 1");
		OrderingOptionEntity option2 = mock(OrderingOptionEntity.class);
		when(option2.getContent()).thenReturn("옵션 2");
		List<OrderingOptionEntity> options = List.of(option1, option2);
		when(orderingOptionEntityRepository.findAllByOrderingQuestionId(1L)).thenReturn(options);

		// when
		QuestionValidateDto result = orderingQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isTrue();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isNull();
		verify(orderingOptionEntityRepository).findAllByOrderingQuestionId(1L);
	}
}
