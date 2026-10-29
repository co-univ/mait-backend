package com.coniv.mait.domain.solve.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

@ExtendWith(MockitoExtension.class)
class AnswerGraderTest {

	@Mock
	private SubmitAnswerValidator submitAnswerValidator;

	@Mock
	private AnswerChecker<Object> checker;

	@Mock
	private QuestionEntity question;

	@Mock
	private SubmitAnswerDto<Object> submittedAnswer;

	@ParameterizedTest
	@EnumSource(QuestionType.class)
	@DisplayName("모든 문제 유형의 답안을 검증한 뒤 해당 채점기에 위임한다")
	void gradeAnswer_validatesBeforeGrading(QuestionType type) {
		// given
		when(checker.getQuestionType()).thenReturn(type);
		when(question.getType()).thenReturn(type);
		when(checker.checkAnswer(question, submittedAnswer)).thenReturn(true);
		AnswerGrader grader = new AnswerGrader(List.of(checker), submitAnswerValidator);

		// when
		boolean result = grader.gradeAnswer(question, submittedAnswer);

		// then
		assertThat(result).isTrue();
		InOrder order = inOrder(submitAnswerValidator, checker);
		order.verify(submitAnswerValidator).validate(question, submittedAnswer);
		order.verify(checker).checkAnswer(question, submittedAnswer);
	}

	@Test
	@DisplayName("검증에 실패한 답안은 채점기에 전달하지 않는다")
	void gradeAnswer_rejectsBeforeGrading() {
		// given
		when(checker.getQuestionType()).thenReturn(QuestionType.SHORT);
		QuestionSolvingException exception = new QuestionSolvingException(QuestionSolveExceptionCode.INVALID_TYPE);
		doThrow(exception).when(submitAnswerValidator).validate(question, submittedAnswer);
		AnswerGrader grader = new AnswerGrader(List.of(checker), submitAnswerValidator);

		// when & then
		assertThatThrownBy(() -> grader.gradeAnswer(question, submittedAnswer)).isSameAs(exception);
		verify(checker, never()).checkAnswer(any(), any());
	}
}
