package com.coniv.mait.domain.solve.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
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
		when(submittedAnswer.getType()).thenReturn(type);
		when(checker.checkAnswer(question, submittedAnswer)).thenReturn(true);
		AnswerGrader grader = new AnswerGrader(List.of(checker));

		// when
		boolean result = grader.gradeAnswer(question, submittedAnswer);

		// then
		assertThat(result).isTrue();
		verify(checker).checkAnswer(question, submittedAnswer);
	}

	@ParameterizedTest
	@CsvSource({
		"MULTIPLE, SHORT", "MULTIPLE, ORDERING", "MULTIPLE, FILL_BLANK",
		"SHORT, MULTIPLE", "SHORT, ORDERING", "SHORT, FILL_BLANK",
		"ORDERING, MULTIPLE", "ORDERING, SHORT", "ORDERING, FILL_BLANK",
		"FILL_BLANK, MULTIPLE", "FILL_BLANK, SHORT", "FILL_BLANK, ORDERING"
	})
	@DisplayName("실제 문제와 제출 유형이 다르면 채점 전에 거절한다")
	void gradeAnswer_rejectsBeforeGrading(QuestionType questionType, QuestionType submittedType) {
		// given
		when(checker.getQuestionType()).thenReturn(questionType);
		when(question.getType()).thenReturn(questionType);
		when(submittedAnswer.getType()).thenReturn(submittedType);
		AnswerGrader grader = new AnswerGrader(List.of(checker));

		// when & then
		assertThatThrownBy(() -> grader.gradeAnswer(question, submittedAnswer))
			.isInstanceOfSatisfying(QuestionSolvingException.class, exception ->
				assertThat(exception.getExceptionCode()).isEqualTo(QuestionSolveExceptionCode.INVALID_TYPE));
		verify(checker, never()).checkAnswer(any(), any());
	}
}
