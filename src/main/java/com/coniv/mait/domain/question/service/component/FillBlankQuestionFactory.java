package com.coniv.mait.domain.question.service.component;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FillBlankQuestionFactory implements QuestionFactory<FillBlankQuestionDto> {

	private final QuestionEntityRepository questionEntityRepository;

	private final FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.FILL_BLANK;
	}

	@Transactional
	@Override
	public QuestionEntity save(FillBlankQuestionDto questionDto, QuestionSetEntity questionSetEntity) {
		FillBlankQuestionEntity question = create(questionDto, questionSetEntity);
		questionEntityRepository.save(question);

		createSubEntities(questionDto, question);
		return question;
	}

	@Override
	public QuestionDto getQuestion(QuestionEntity question, boolean answerVisible) {
		List<FillBlankAnswerEntity> fillBlankAnswers = fillBlankAnswerEntityRepository
			.findAllByFillBlankQuestionId(question.getId());
		return FillBlankQuestionDto.of((FillBlankQuestionEntity)question, fillBlankAnswers, answerVisible);
	}

	@Override
	public void deleteSubEntities(QuestionEntity question) {
		fillBlankAnswerEntityRepository.deleteAllByFillBlankQuestionId(question.getId());
	}

	@Override
	public void createSubEntities(FillBlankQuestionDto questionDto, QuestionEntity question) {
		List<FillBlankAnswerEntity> fillBlankAnswers = createFillBlankAnswers(questionDto.getAnswers(),
			(FillBlankQuestionEntity)question);
		fillBlankAnswerEntityRepository.saveAll(fillBlankAnswers);
	}

	@Override
	public FillBlankQuestionEntity create(FillBlankQuestionDto dto, QuestionSetEntity questionSetEntity) {
		return FillBlankQuestionEntity.builder()
			.number(dto.getNumber())
			.questionSet(questionSetEntity)
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.build();
	}

	@Override
	@Transactional
	public FillBlankQuestionEntity createDefaultQuestion(String lexoRank, QuestionSetEntity questionSetEntity) {
		return questionEntityRepository.save(FillBlankQuestionEntity.builder()
			.lexoRank(lexoRank)
			.questionSet(questionSetEntity)
			.build());
	}

	public List<FillBlankAnswerEntity> createFillBlankAnswers(
		List<FillBlankAnswerDto> fillBlankAnswerDtos, FillBlankQuestionEntity fillBlankQuestionEntity) {
		validateMainAnswers(fillBlankAnswerDtos);
		return fillBlankAnswerDtos.stream()
			.map(dto -> createFillBlankAnswerEntity(dto, fillBlankQuestionEntity.getId()))
			.toList();
	}

	private FillBlankAnswerEntity createFillBlankAnswerEntity(FillBlankAnswerDto dto, Long questionId) {
		return FillBlankAnswerEntity.builder()
			.answer(dto.getAnswer())
			.isMain(dto.isMain())
			.number(dto.getNumber())
			.fillBlankQuestionId(questionId)
			.build();
	}

	private void validateMainAnswers(List<FillBlankAnswerDto> answers) {
		answers.stream()
			.collect(Collectors.groupingBy(FillBlankAnswerDto::getNumber))
			.forEach(this::validateSingleMainAnswer);
	}

	private void validateSingleMainAnswer(Long number, List<FillBlankAnswerDto> answersForNumber) {
		long mainCount = answersForNumber.stream()
			.mapToLong(answer -> answer.isMain() ? 1 : 0)
			.sum();

		if (mainCount != 1) {
			throw new UserParameterException(
				String.format("Each blank number must have exactly one main answer. Number %d has %d main answers.",
					number, mainCount));
		}
	}
}
