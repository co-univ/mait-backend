package com.coniv.mait.domain.solve.service.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.service.dto.FillBlankQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.FillBlankSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.OrderingQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.ShortQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

@Component
public class SubmitAnswerValidator {

	public void validate(QuestionEntity question, SubmitAnswerDto<?> submittedAnswer) {
		if (submittedAnswer == null) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.INVALID_ANSWER_FORMAT);
		}
		if (question.getType() != submittedAnswer.getType()) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.INVALID_TYPE);
		}

		List<?> answers = submittedAnswer.getSubmitAnswers();
		if (answers == null) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.INVALID_ANSWER_FORMAT);
		}

		boolean valid = switch (question.getType()) {
			case MULTIPLE -> submittedAnswer instanceof MultipleQuestionSubmitAnswer
				&& answers.stream().allMatch(Long.class::isInstance);
			case SHORT -> submittedAnswer instanceof ShortQuestionSubmitAnswer
				&& answers.stream().allMatch(String.class::isInstance);
			case ORDERING -> submittedAnswer instanceof OrderingQuestionSubmitAnswer
				&& answers.stream().allMatch(Long.class::isInstance);
			case FILL_BLANK -> submittedAnswer instanceof FillBlankQuestionSubmitAnswer
				&& answers.stream().allMatch(answer -> answer instanceof FillBlankSubmitAnswer blank
				&& blank.number() != null && blank.answer() != null);
		};
		if (!valid) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.INVALID_ANSWER_FORMAT);
		}
	}
}
