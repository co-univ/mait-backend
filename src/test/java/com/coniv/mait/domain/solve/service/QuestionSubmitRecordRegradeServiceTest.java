package com.coniv.mait.domain.solve.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionExceptionCode;
import com.coniv.mait.domain.question.exception.QuestionStatusException;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

@ExtendWith(MockitoExtension.class)
class QuestionSubmitRecordRegradeServiceTest {

	@Mock
	private QuestionReader questionReader;

	@Mock
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Mock
	private AnswerGrader answerGrader;

	@InjectMocks
	private QuestionSubmitRecordRegradeService questionSubmitRecordRegradeService;

	@Test
	@DisplayName("재채점 테스트 - 성공")
	void regradeSubmitRecords_shortQuestion_updatesSubmitRecords() {
		// given
		Long questionId = 10L;

		QuestionEntity question = mock(QuestionEntity.class);
		doReturn(questionId).when(question).getId();
		doReturn(QuestionType.SHORT).when(question).getType();
		doReturn(question).when(questionReader).getQuestion(questionId);

		AnswerSubmitRecordEntity record1 = mock(AnswerSubmitRecordEntity.class);
		AnswerSubmitRecordEntity record2 = mock(AnswerSubmitRecordEntity.class);

		doReturn("{\"type\":\"SHORT\",\"submitAnswers\":[\"A\"]}").when(record1).getSubmittedAnswer();
		doReturn("{\"type\":\"SHORT\",\"submitAnswers\":[\"B\"]}").when(record2).getSubmittedAnswer();

		doReturn(List.of(record1, record2)).when(answerSubmitRecordEntityRepository).findAllByQuestionId(questionId);

		doReturn(true)
			.doReturn(false)
			.when(answerGrader)
			.gradeAnswer(eq(question), any(SubmitAnswerDto.class));

		// when
		questionSubmitRecordRegradeService.regradeSubmitRecords(questionId);

		// then
		verify(answerSubmitRecordEntityRepository).findAllByQuestionId(questionId);
		verify(answerGrader, times(2)).gradeAnswer(eq(question), any(SubmitAnswerDto.class));
		verify(record1).updateCorrect(true);
		verify(record2).updateCorrect(false);
	}

	@Test
	@DisplayName("재채점 테스트 - 지원하지 않는 타입")
	void regradeSubmitRecords_unavailableQuestionType_throws() {
		// given
		Long questionId = 10L;
		QuestionEntity question = mock(QuestionEntity.class);
		doReturn(QuestionType.MULTIPLE).when(question).getType();
		doReturn(question).when(questionReader).getQuestion(questionId);

		// when & then
		assertThatThrownBy(() -> questionSubmitRecordRegradeService.regradeSubmitRecords(questionId))
			.isInstanceOf(QuestionStatusException.class)
			.satisfies(ex -> {
				QuestionStatusException qse = (QuestionStatusException)ex;
				assertThat(qse.getQuestionExceptionCode())
					.isEqualTo(QuestionExceptionCode.UNAVAILABLE_TYPE);
			});
	}
}

