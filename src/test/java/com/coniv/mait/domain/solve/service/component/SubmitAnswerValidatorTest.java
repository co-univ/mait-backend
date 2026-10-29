package com.coniv.mait.domain.solve.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
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
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.dto.FillBlankQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.FillBlankSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.OrderingQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.ShortQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

@ExtendWith(MockitoExtension.class)
class SubmitAnswerValidatorTest {

	@InjectMocks
	private SubmitAnswerValidator validator;

	@Mock
	private QuestionEntity question;

	@ParameterizedTest
	@MethodSource("validAnswers")
	@DisplayName("문제 유형과 답안 구조가 일치하면 통과한다")
	void validate_validAnswer(QuestionType type, SubmitAnswerDto<?> answer) {
		// given
		when(question.getType()).thenReturn(type);

		// when & then
		assertThatCode(() -> validator.validate(question, answer)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@MethodSource("mismatchedTypes")
	@DisplayName("문제 유형과 제출 유형이 다르면 INVALID_TYPE으로 거절한다")
	void validate_mismatchedType(QuestionType type, SubmitAnswerDto<?> answer) {
		// given
		when(question.getType()).thenReturn(type);

		// when & then
		assertThatThrownBy(() -> validator.validate(question, answer))
			.isInstanceOfSatisfying(QuestionSolvingException.class, exception ->
				assertThat(exception.getExceptionCode()).isEqualTo(QuestionSolveExceptionCode.INVALID_TYPE));
	}

	@ParameterizedTest
	@MethodSource("invalidAnswers")
	@DisplayName("답안 목록이나 원소의 구조가 잘못되면 형식 오류로 거절한다")
	void validate_invalidStructure(QuestionType type, SubmitAnswerDto<?> answer) {
		// given
		when(question.getType()).thenReturn(type);

		// when & then
		assertThatThrownBy(() -> validator.validate(question, answer))
			.isInstanceOfSatisfying(QuestionSolvingException.class, exception ->
				assertThat(exception.getExceptionCode()).isEqualTo(QuestionSolveExceptionCode.INVALID_ANSWER_FORMAT));
	}

	@ParameterizedTest
	@MethodSource("incompleteDrafts")
	@DisplayName("임시 저장을 위한 빈 목록과 미완성 문자열은 허용한다")
	void validate_incompleteDraft(QuestionType type, SubmitAnswerDto<?> answer) {
		// given
		when(question.getType()).thenReturn(type);

		// when & then
		assertThatCode(() -> validator.validate(question, answer)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("제출 답안 자체가 없으면 형식 오류로 거절한다")
	void validate_nullAnswer() {
		// when & then
		assertThatThrownBy(() -> validator.validate(question, null))
			.isInstanceOfSatisfying(QuestionSolvingException.class, exception ->
				assertThat(exception.getExceptionCode()).isEqualTo(QuestionSolveExceptionCode.INVALID_ANSWER_FORMAT));
	}

	@Test
	@DisplayName("유형 값이 같아도 지원하지 않는 DTO 구현은 거절한다")
	void validate_unsupportedDtoImplementation() {
		// given
		when(question.getType()).thenReturn(QuestionType.MULTIPLE);
		SubmitAnswerDto<Long> answer = new SubmitAnswerDto<>() {
			@Override
			public QuestionType getType() {
				return QuestionType.MULTIPLE;
			}

			@Override
			public List<Long> getSubmitAnswers() {
				return List.of(1L);
			}
		};

		// when & then
		assertThatThrownBy(() -> validator.validate(question, answer))
			.isInstanceOfSatisfying(QuestionSolvingException.class, exception ->
				assertThat(exception.getExceptionCode()).isEqualTo(QuestionSolveExceptionCode.INVALID_ANSWER_FORMAT));
	}

	static Stream<Arguments> validAnswers() {
		return Stream.of(
			Arguments.of(QuestionType.MULTIPLE, new MultipleQuestionSubmitAnswer(List.of(1L))),
			Arguments.of(QuestionType.SHORT, new ShortQuestionSubmitAnswer(List.of("답안"))),
			Arguments.of(QuestionType.ORDERING, new OrderingQuestionSubmitAnswer(List.of(2L, 1L))),
			Arguments.of(QuestionType.FILL_BLANK, new FillBlankQuestionSubmitAnswer(
				List.of(new FillBlankSubmitAnswer(1L, "답안"))))
		);
	}

	static Stream<Arguments> mismatchedTypes() {
		return validAnswers().flatMap(arguments -> Arrays.stream(QuestionType.values())
			.filter(type -> type != arguments.get()[0])
			.map(type -> Arguments.of(type, arguments.get()[1])));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static Stream<Arguments> invalidAnswers() {
		return Stream.of(
			Arguments.of(QuestionType.MULTIPLE, new MultipleQuestionSubmitAnswer(null)),
			Arguments.of(QuestionType.SHORT, new ShortQuestionSubmitAnswer(null)),
			Arguments.of(QuestionType.ORDERING, new OrderingQuestionSubmitAnswer(null)),
			Arguments.of(QuestionType.FILL_BLANK, new FillBlankQuestionSubmitAnswer(null)),
			Arguments.of(QuestionType.MULTIPLE, new MultipleQuestionSubmitAnswer((List)List.of("1"))),
			Arguments.of(QuestionType.SHORT, new ShortQuestionSubmitAnswer((List)List.of(1L))),
			Arguments.of(QuestionType.ORDERING, new OrderingQuestionSubmitAnswer((List)List.of("1"))),
			Arguments.of(QuestionType.FILL_BLANK, new FillBlankQuestionSubmitAnswer((List)List.of("답안"))),
			Arguments.of(QuestionType.MULTIPLE, new MultipleQuestionSubmitAnswer(Arrays.asList((Long)null))),
			Arguments.of(QuestionType.SHORT, new ShortQuestionSubmitAnswer(Arrays.asList((String)null))),
			Arguments.of(QuestionType.ORDERING, new OrderingQuestionSubmitAnswer(Arrays.asList((Long)null))),
			Arguments.of(QuestionType.FILL_BLANK, new FillBlankQuestionSubmitAnswer(
				Arrays.asList((FillBlankSubmitAnswer)null))),
			Arguments.of(QuestionType.FILL_BLANK, new FillBlankQuestionSubmitAnswer(
				List.of(new FillBlankSubmitAnswer(null, "답안")))),
			Arguments.of(QuestionType.FILL_BLANK, new FillBlankQuestionSubmitAnswer(
				List.of(new FillBlankSubmitAnswer(1L, null))))
		);
	}

	static Stream<Arguments> incompleteDrafts() {
		return Stream.of(
			Arguments.of(QuestionType.MULTIPLE, new MultipleQuestionSubmitAnswer(List.of())),
			Arguments.of(QuestionType.SHORT, new ShortQuestionSubmitAnswer(List.of())),
			Arguments.of(QuestionType.ORDERING, new OrderingQuestionSubmitAnswer(List.of())),
			Arguments.of(QuestionType.FILL_BLANK, new FillBlankQuestionSubmitAnswer(List.of())),
			Arguments.of(QuestionType.SHORT, new ShortQuestionSubmitAnswer(List.of("", "답안"))),
			Arguments.of(QuestionType.FILL_BLANK, new FillBlankQuestionSubmitAnswer(
				List.of(new FillBlankSubmitAnswer(1L, ""), new FillBlankSubmitAnswer(2L, "답안"))))
		);
	}
}
