package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.enums.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.global.exception.custom.UserParameterException;

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

	@ParameterizedTest
	@MethodSource("acceptedAnswerAdditions")
	@DisplayName("기존 그룹에 인정답안을 추가하며 서로 다른 그룹 간 같은 답안도 허용한다")
	void validateAcceptedAnswerAddition_valid(List<ShortAnswerDto> newAnswers) {
		// given
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").isMain(true).build(),
			ShortAnswerEntity.builder().number(1L).answer("beta").isMain(false).build(),
			ShortAnswerEntity.builder().number(2L).answer("delta").isMain(true).build()
		));

		// when & then
		assertThatCode(() -> shortQuestionValidator.validateAcceptedAnswerAddition(1L, newAnswers))
			.doesNotThrowAnyException();
		verify(shortAnswerEntityRepository).findAllByShortQuestionId(1L);
	}

	@ParameterizedTest
	@MethodSource("invalidAcceptedAnswerAdditions")
	@DisplayName("존재하지 않는 그룹과 정규화한 기존·요청 내 중복을 거절한다")
	void validateAcceptedAnswerAddition_invalid(List<ShortAnswerDto> newAnswers, String message) {
		// given
		when(shortAnswerEntityRepository.findAllByShortQuestionId(1L)).thenReturn(List.of(
			ShortAnswerEntity.builder().number(1L).answer("alpha").isMain(true).build(),
			ShortAnswerEntity.builder().number(1L).answer("beta").isMain(false).build(),
			ShortAnswerEntity.builder().number(2L).answer("delta").isMain(true).build()
		));

		// when & then
		assertThatThrownBy(() -> shortQuestionValidator.validateAcceptedAnswerAddition(1L, newAnswers))
			.isInstanceOf(UserParameterException.class).hasMessage(message);
		verify(shortAnswerEntityRepository, never()).saveAll(any());
	}

	static Stream<Arguments> acceptedAnswerAdditions() {
		return Stream.of(
			Arguments.of(List.of(
				ShortAnswerDto.builder().number(1L).answer("gamma").main(false).build(),
				ShortAnswerDto.builder().number(2L).answer("epsilon").main(false).build())),
			Arguments.of(List.of(
				ShortAnswerDto.builder().number(1L).answer("gamma").main(false).build(),
				ShortAnswerDto.builder().number(2L).answer("gamma").main(false).build()))
		);
	}

	static Stream<Arguments> invalidAcceptedAnswerAdditions() {
		return Stream.of(
			Arguments.of(List.of(
				ShortAnswerDto.builder().number(1L).answer("gamma").main(false).build(),
				ShortAnswerDto.builder().number(3L).answer("epsilon").main(false).build()), "해당 문제에 존재하지 않는 정답 그룹입니다."),
			Arguments.of(List.of(
				ShortAnswerDto.builder().number(1L).answer(" ALPHA ").main(false).build()), "같은 정답 그룹에 이미 존재하는 답안입니다."),
			Arguments.of(List.of(
				ShortAnswerDto.builder().number(1L).answer(" BETA ").main(false).build()), "같은 정답 그룹에 이미 존재하는 답안입니다."),
			Arguments.of(List.of(
				ShortAnswerDto.builder().number(1L).answer("gamma").main(false).build(),
				ShortAnswerDto.builder().number(1L).answer(" GAMMA ").main(false).build()), "같은 정답 그룹에 이미 존재하는 답안입니다.")
		);
	}
}
