package com.coniv.mait.domain.question.service.component;

import static com.coniv.mait.domain.question.constant.QuestionConstant.*;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MultipleQuestionFactory implements QuestionFactory<MultipleQuestionDto> {

	private final QuestionEntityRepository questionEntityRepository;

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.MULTIPLE;
	}

	@Transactional
	@Override
	public QuestionEntity save(MultipleQuestionDto questionDto, QuestionSetEntity questionSetEntity) {
		MultipleQuestionEntity question = create(questionDto, questionSetEntity);
		questionEntityRepository.save(question);

		createSubEntities(questionDto, question);
		return question;
	}

	@Override
	public QuestionDto getQuestion(QuestionEntity question, boolean answerVisible) {
		List<MultipleChoiceEntity> choices = multipleChoiceEntityRepository.findAllByQuestionId(question.getId());
		return MultipleQuestionDto.of((MultipleQuestionEntity)question, choices, answerVisible);
	}

	@Override
	public void deleteSubEntities(QuestionEntity question) {
		multipleChoiceEntityRepository.deleteAllByQuestionId(question.getId());
	}

	@Override
	public void createSubEntities(MultipleQuestionDto questionDto, QuestionEntity question) {
		List<MultipleChoiceEntity> choices = createChoices(questionDto.getChoices(), (MultipleQuestionEntity)question);
		multipleChoiceEntityRepository.saveAll(choices);

		MultipleQuestionEntity multipleQuestion = (MultipleQuestionEntity)question;
		multipleQuestion.updateAnswerCount(calculateAnswerCount(questionDto.getChoices()));
	}

	public MultipleQuestionEntity create(MultipleQuestionDto dto, QuestionSetEntity questionSet) {
		return MultipleQuestionEntity.builder()
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.number(dto.getNumber())
			.displayDelayMilliseconds(RandomUtil.getRandomNumber(MAX_DISPLAY_DELAY_MILLISECONDS))
			.questionSet(questionSet)
			.answerCount(calculateAnswerCount(dto.getChoices()))
			.build();
	}

	public List<MultipleChoiceEntity> createChoices(
		List<MultipleChoiceDto> dtos,
		MultipleQuestionEntity question
	) {
		checkChoicesNumber(dtos);
		return dtos.stream()
			.map(dto -> createChoice(dto, question))
			.toList();
	}

	private void checkChoicesNumber(List<MultipleChoiceDto> dtos) {
		long count = dtos.stream()
			.map(MultipleChoiceDto::getNumber)
			.distinct()
			.count();

		if (count != dtos.size()) {
			throw new UserParameterException("중복된 선택지 번호가 존재합니다.");
		}
	}

	private MultipleChoiceEntity createChoice(MultipleChoiceDto dto, MultipleQuestionEntity question) {
		return MultipleChoiceEntity.builder()
			.number(dto.getNumber())
			.content(dto.getContent())
			.question(question)
			.isCorrect(dto.getIsCorrect())
			.build();
	}

	private int calculateAnswerCount(List<MultipleChoiceDto> choices) {
		return (int)choices.stream().filter(MultipleChoiceDto::getIsCorrect).count();
	}
}
