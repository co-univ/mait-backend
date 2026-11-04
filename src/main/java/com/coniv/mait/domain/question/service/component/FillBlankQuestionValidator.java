package com.coniv.mait.domain.question.service.component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.enums.QuestionValidationResult;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;
import com.coniv.mait.domain.solve.util.AnswerProcessUtil;
import com.coniv.mait.global.exception.custom.UserParameterException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FillBlankQuestionValidator implements QuestionValidator {

	private static final int MAX_SUB_ANSWER_COUNT = 5;

	private final FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.FILL_BLANK;
	}

	@Override
	public QuestionValidateDto validate(QuestionEntity question) {
		if (!QuestionValidator.isQuestionValid(question)) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.EMPTY_CONTENT);
		}

		List<FillBlankAnswerEntity> fillBlankAnswers = fillBlankAnswerEntityRepository.findAllByFillBlankQuestionId(
			question.getId());

		if (fillBlankAnswers.stream().noneMatch(FillBlankAnswerEntity::isMain)) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.INVALID_BLANK_COUNT);
		}

		if (fillBlankAnswers.stream().anyMatch(answer -> answer.getAnswer().isBlank())) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.EMPTY_FILL_BLANK_ANSWER_CONTENT);
		}

		Map<Long, Long> numberByCount = fillBlankAnswers.stream()
			.filter(shortAnswer -> !shortAnswer.isMain())
			.collect(Collectors.groupingBy(FillBlankAnswerEntity::getNumber, Collectors.counting()));

		if (numberByCount.values().stream().anyMatch(cnt -> cnt > MAX_SUB_ANSWER_COUNT)) {
			return QuestionValidateDto.invalid(question, QuestionValidationResult.INVALID_FILL_BLANK_SUB_ANSWER_COUNT);
		}

		return QuestionValidateDto.valid(question);
	}

	public void validateAcceptedAnswerAddition(Long questionId, List<FillBlankAnswerDto> newAnswers) {
		Map<Long, Set<String>> answersByNumber = fillBlankAnswerEntityRepository
			.findAllByFillBlankQuestionId(questionId).stream()
			.collect(Collectors.groupingBy(FillBlankAnswerEntity::getNumber,
				Collectors.mapping(answer -> AnswerProcessUtil.processAnswer(answer.getAnswer()),
					Collectors.toCollection(HashSet::new))));

		for (FillBlankAnswerDto answer : newAnswers) {
			Set<String> acceptedAnswers = answersByNumber.get(answer.getNumber());
			if (acceptedAnswers == null) {
				throw new UserParameterException("해당 문제에 존재하지 않는 빈칸 번호입니다.");
			}
			if (!acceptedAnswers.add(AnswerProcessUtil.processAnswer(answer.getAnswer()))) {
				throw new UserParameterException("같은 빈칸에 이미 존재하는 답안입니다.");
			}
		}
	}
}
