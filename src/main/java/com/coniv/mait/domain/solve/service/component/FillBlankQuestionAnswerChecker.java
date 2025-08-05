package com.coniv.mait.domain.solve.service.component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.solve.service.dto.FillBlankSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FillBlankQuestionAnswerChecker implements AnswerChecker<FillBlankSubmitAnswer> {

	private final FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.FILL_BLANK;
	}

	@Override
	public boolean checkAnswer(QuestionEntity question, SubmitAnswerDto<FillBlankSubmitAnswer> answers) {
		Map<Long, Set<String>> answersByNumber = fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(
				question.getId())
			.stream()
			.collect(Collectors.groupingBy(FillBlankAnswerEntity::getNumber,
				Collectors.mapping(FillBlankAnswerEntity::getAnswer, Collectors.toSet())));

		List<FillBlankSubmitAnswer> submitAnswers = answers.getSubmitAnswers();

		for (FillBlankSubmitAnswer submitAnswer : submitAnswers) {
			Set<String> correctAnswers = answersByNumber.get(submitAnswer.number());

			if (correctAnswers == null || correctAnswers.isEmpty()) {
				return false;
			}

			if (!correctAnswers.contains(submitAnswer.answer())) {
				return false;
			}
		}

		return true;
	}
}
