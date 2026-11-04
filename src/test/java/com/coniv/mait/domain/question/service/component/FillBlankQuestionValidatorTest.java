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

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.enums.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;
import com.coniv.mait.global.exception.custom.UserParameterException;

@ExtendWith(MockitoExtension.class)
class FillBlankQuestionValidatorTest {

	@InjectMocks
	private FillBlankQuestionValidator fillBlankQuestionValidator;

	@Mock
	private FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Test
	@DisplayName("getQuestionType - FILL_BLANK 타입 반환")
	void getQuestionType() {
		// when
		QuestionType result = fillBlankQuestionValidator.getQuestionType();

		// then
		assertThat(result).isEqualTo(QuestionType.FILL_BLANK);
	}

	@Test
	@DisplayName("validate - 유효한 빈칸 문제 검증 성공")
	void validate_ValidQuestion_ReturnsValid() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("빈칸 문제 내용");

		FillBlankAnswerEntity mainAnswer = mock(FillBlankAnswerEntity.class);
		when(mainAnswer.isMain()).thenReturn(true);
		when(mainAnswer.getAnswer()).thenReturn("정답");

		FillBlankAnswerEntity subAnswer = mock(FillBlankAnswerEntity.class);
		when(subAnswer.getAnswer()).thenReturn("부정답");

		List<FillBlankAnswerEntity> answers = List.of(mainAnswer, subAnswer);
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(answers);

		// when
		QuestionValidateDto result = fillBlankQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isTrue();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isNull();
		verify(fillBlankAnswerEntityRepository).findAllByFillBlankQuestionId(1L);
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
		QuestionValidateDto result = fillBlankQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_CONTENT);
		verify(fillBlankAnswerEntityRepository, never()).findAllByFillBlankQuestionId(anyLong());
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
		QuestionValidateDto result = fillBlankQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_CONTENT);
		verify(fillBlankAnswerEntityRepository, never()).findAllByFillBlankQuestionId(anyLong());
	}

	@Test
	@DisplayName("validate - 메인 답변이 없으면 INVALID_BLANK_COUNT 반환")
	void validate_NoMainAnswer_ReturnsInvalidBlankCount() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("빈칸 문제 내용");

		FillBlankAnswerEntity answer1 = mock(FillBlankAnswerEntity.class);
		when(answer1.isMain()).thenReturn(false);

		FillBlankAnswerEntity answer2 = mock(FillBlankAnswerEntity.class);
		when(answer2.isMain()).thenReturn(false);

		List<FillBlankAnswerEntity> answers = List.of(answer1, answer2);
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(answers);

		// when
		QuestionValidateDto result = fillBlankQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.INVALID_BLANK_COUNT);
		verify(fillBlankAnswerEntityRepository).findAllByFillBlankQuestionId(1L);
	}

	@Test
	@DisplayName("validate - 답변이 빈 문자열이면 EMPTY_FILL_BLANK_ANSWER_CONTENT 반환")
	void validate_BlankAnswer_ReturnsEmptyFillBlankAnswerContent() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("빈칸 문제 내용");

		FillBlankAnswerEntity mainAnswer = mock(FillBlankAnswerEntity.class);
		when(mainAnswer.isMain()).thenReturn(true);
		when(mainAnswer.getAnswer()).thenReturn("   ");

		List<FillBlankAnswerEntity> answers = List.of(mainAnswer);
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(answers);

		// when
		QuestionValidateDto result = fillBlankQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.EMPTY_FILL_BLANK_ANSWER_CONTENT);
		verify(fillBlankAnswerEntityRepository).findAllByFillBlankQuestionId(1L);
	}

	@Test
	@DisplayName("validate - 답변 리스트가 비어있으면 INVALID_BLANK_COUNT 반환")
	void validate_EmptyAnswerList_ReturnsInvalidBlankCount() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("빈칸 문제 내용");

		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(List.of());

		// when
		QuestionValidateDto result = fillBlankQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isFalse();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isEqualTo(QuestionValidationResult.INVALID_BLANK_COUNT);
		verify(fillBlankAnswerEntityRepository).findAllByFillBlankQuestionId(1L);
	}

	@Test
	@DisplayName("validate - 메인 답변만 있어도 유효함")
	void validate_OnlyMainAnswer_ReturnsValid() {
		// given
		QuestionEntity question = mock(QuestionEntity.class);
		when(question.getId()).thenReturn(1L);
		when(question.getNumber()).thenReturn(1L);
		when(question.getContent()).thenReturn("빈칸 문제 내용");

		FillBlankAnswerEntity mainAnswer = mock(FillBlankAnswerEntity.class);
		when(mainAnswer.isMain()).thenReturn(true);
		when(mainAnswer.getAnswer()).thenReturn("정답");

		List<FillBlankAnswerEntity> answers = List.of(mainAnswer);
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(answers);

		// when
		QuestionValidateDto result = fillBlankQuestionValidator.validate(question);

		// then
		assertThat(result.isValid()).isTrue();
		assertThat(result.getQuestionId()).isEqualTo(1L);
		assertThat(result.getNumber()).isEqualTo(1L);
		assertThat(result.getReason()).isNull();
		verify(fillBlankAnswerEntityRepository).findAllByFillBlankQuestionId(1L);
	}

	@ParameterizedTest
	@MethodSource("acceptedAnswerAdditions")
	@DisplayName("기존 그룹에 인정답안을 추가하며 서로 다른 그룹 간 같은 답안도 허용한다")
	void validateAcceptedAnswerAddition_valid(List<FillBlankAnswerDto> newAnswers) {
		// given
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(List.of(
			FillBlankAnswerEntity.builder().number(1L).answer("alpha").isMain(true).build(),
			FillBlankAnswerEntity.builder().number(1L).answer("beta").isMain(false).build(),
			FillBlankAnswerEntity.builder().number(2L).answer("delta").isMain(true).build()
		));

		// when & then
		assertThatCode(() -> fillBlankQuestionValidator.validateAcceptedAnswerAddition(1L, newAnswers))
			.doesNotThrowAnyException();
		verify(fillBlankAnswerEntityRepository).findAllByFillBlankQuestionId(1L);
	}

	@ParameterizedTest
	@MethodSource("invalidAcceptedAnswerAdditions")
	@DisplayName("존재하지 않는 그룹과 정규화한 기존·요청 내 중복을 거절한다")
	void validateAcceptedAnswerAddition_invalid(List<FillBlankAnswerDto> newAnswers, String message) {
		// given
		when(fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(1L)).thenReturn(List.of(
			FillBlankAnswerEntity.builder().number(1L).answer("alpha").isMain(true).build(),
			FillBlankAnswerEntity.builder().number(1L).answer("beta").isMain(false).build(),
			FillBlankAnswerEntity.builder().number(2L).answer("delta").isMain(true).build()
		));

		// when & then
		assertThatThrownBy(() -> fillBlankQuestionValidator.validateAcceptedAnswerAddition(1L, newAnswers))
			.isInstanceOf(UserParameterException.class).hasMessage(message);
		verify(fillBlankAnswerEntityRepository, never()).saveAll(any());
	}

	static Stream<Arguments> acceptedAnswerAdditions() {
		return Stream.of(
			Arguments.of(List.of(
				FillBlankAnswerDto.builder().number(1L).answer("gamma").main(false).build(),
				FillBlankAnswerDto.builder().number(2L).answer("epsilon").main(false).build())),
			Arguments.of(List.of(
				FillBlankAnswerDto.builder().number(1L).answer("gamma").main(false).build(),
				FillBlankAnswerDto.builder().number(2L).answer("gamma").main(false).build()))
		);
	}

	static Stream<Arguments> invalidAcceptedAnswerAdditions() {
		return Stream.of(
			Arguments.of(List.of(
				FillBlankAnswerDto.builder().number(1L).answer("gamma").main(false).build(),
				FillBlankAnswerDto.builder().number(3L).answer("epsilon").main(false).build()),
				"해당 문제에 존재하지 않는 빈칸 번호입니다."),
			Arguments.of(List.of(
				FillBlankAnswerDto.builder().number(1L).answer(" ALPHA ").main(false).build()),
				"같은 빈칸에 이미 존재하는 답안입니다."),
			Arguments.of(List.of(
				FillBlankAnswerDto.builder().number(1L).answer(" BETA ").main(false).build()),
				"같은 빈칸에 이미 존재하는 답안입니다."),
			Arguments.of(List.of(
				FillBlankAnswerDto.builder().number(1L).answer("gamma").main(false).build(),
				FillBlankAnswerDto.builder().number(1L).answer(" GAMMA ").main(false).build()), "같은 빈칸에 이미 존재하는 답안입니다.")
		);
	}
}
