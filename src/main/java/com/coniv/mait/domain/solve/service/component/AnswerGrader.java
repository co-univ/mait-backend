package com.coniv.mait.domain.solve.service.component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

@Component
public class AnswerGrader {

	private final Map<QuestionType, AnswerChecker> answerCheckers;

	@Autowired
	public AnswerGrader(List<AnswerChecker> checkers) {
		answerCheckers = checkers.stream()
			.collect(Collectors.toUnmodifiableMap(AnswerChecker::getQuestionType, Function.identity()));
	}

	public boolean gradeAnswer(QuestionEntity question, SubmitAnswerDto submitAnswer) {
		return answerCheckers.get(question.getType()).checkAnswer(question, submitAnswer);
	}
}
