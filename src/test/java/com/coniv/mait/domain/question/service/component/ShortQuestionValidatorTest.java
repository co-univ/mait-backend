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

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

@ExtendWith(MockitoExtension.class)
class ShortQuestionValidatorTest {

	@InjectMocks
	private ShortQuestionValidator shortQuestionValidator;

	@Mock
	private ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Test
	@DisplayName("getQuestionType - SHORT 타입 반환")
	void getQuestionType() {
		// when
		QuestionType result = shortQuestionValidator.getQuestionType();

		// then
		assertThat(result).isEqualTo(QuestionType.SHORT);
	}

	@Test
	@DisplayName("validate - 유효한 주관식 문제 검증 성공")
	void validate_ValidQuestion_ReturnsValid() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("주관식 문제 내용");

		ShortAnswerEntity mainAnswer = mock(ShortAnswerEntity.class);
		when(mainAnswer.isMain()).thenReturn(true);
		when(mainAnswer.getAnswer()).thenReturn("정답");

		ShortAnswerEntity subAnswer = mock(ShortAnswerEntity.class);
		when(subAnswer.getAnswer()).thenReturn("부정답");

		List<ShortAnswerEntity> answers = List.of(mainAnswer, subAnswer);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(answers);

		// when
		QuestionValidateDto result = shortQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isTrue();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isNull();
		verify(shortAnswerEntityRepository).findAllByShortQuestionId(1L);
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
		QuestionValidateDto result = shortQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_CONTENT);
		verify(shortAnswerEntityRepository, never()).findAllByShortQuestionId(anyLong());
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
		QuestionValidateDto result = shortQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_CONTENT);
		verify(shortAnswerEntityRepository, never()).findAllByShortQuestionId(anyLong());
	}

	@Test
	@DisplayName("validate - 메인 답변이 없으면 INVALID_ANSWER_COUNT 반환")
	void validate_NoMainAnswer_ReturnsInvalidAnswerCount() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("주관식 문제 내용");

		ShortAnswerEntity answer1 = mock(ShortAnswerEntity.class);
		when(answer1.isMain()).thenReturn(false);

		ShortAnswerEntity answer2 = mock(ShortAnswerEntity.class);
		when(answer2.isMain()).thenReturn(false);

		List<ShortAnswerEntity> answers = List.of(answer1, answer2);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(answers);

		// when
		QuestionValidateDto result = shortQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.INVALID_ANSWER_COUNT);
		verify(shortAnswerEntityRepository).findAllByShortQuestionId(1L);
	}

	@Test
	@DisplayName("validate - 답변이 빈 문자열이면 EMPTY_SHORT_ANSWER_CONTENT 반환")
	void validate_BlankAnswer_ReturnsEmptyShortAnswerContent() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("주관식 문제 내용");

		ShortAnswerEntity mainAnswer = mock(ShortAnswerEntity.class);
		when(mainAnswer.isMain()).thenReturn(true);
		when(mainAnswer.getAnswer()).thenReturn("   ");

		List<ShortAnswerEntity> answers = List.of(mainAnswer);
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(answers);

		// when
		QuestionValidateDto result = shortQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_SHORT_ANSWER_CONTENT);
		verify(shortAnswerEntityRepository).findAllByShortQuestionId(1L);
	}

	@Test
	@DisplayName("validate - 답변 리스트가 비어있으면 INVALID_ANSWER_COUNT 반환")
	void validate_EmptyAnswerList_ReturnsInvalidAnswerCount() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("주관식 문제 내용");

		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(List.of());

		// when
		QuestionValidateDto result = shortQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.INVALID_ANSWER_COUNT);
		verify(shortAnswerEntityRepository).findAllByShortQuestionId(1L);
	}
}
