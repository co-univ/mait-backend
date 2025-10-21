package com.coniv.mait.domain.question.service.component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

@Component
public class QuestionChecker {

	private final Map<QuestionType, QuestionValidator> validators;

	@Autowired
	public QuestionChecker(List<QuestionValidator> questionValidators) {
		this.validators = questionValidators.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionValidator::getQuestionType, Function.identity()));
	}

	public QuestionValidateDto validateQuestion(QuestionEntity question) {
		QuestionValidator questionValidator = validators.get(question.getType());
		return questionValidator.validate(question);
	}
}
