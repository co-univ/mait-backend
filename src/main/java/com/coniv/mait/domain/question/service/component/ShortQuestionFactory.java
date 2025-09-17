package com.coniv.mait.domain.question.service.component;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.constant.QuestionConstant;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.coniv.mait.global.exception.custom.UserParameterException;
import com.coniv.mait.global.util.RandomUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShortQuestionFactory implements QuestionFactory<ShortQuestionDto> {

	private final QuestionEntityRepository questionEntityRepository;

	private final ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.SHORT;
	}

	@Transactional
	@Override
	public void save(ShortQuestionDto questionDto, QuestionSetEntity questionSetEntity) {
		ShortQuestionEntity question = create(questionDto, questionSetEntity);
		List<ShortAnswerEntity> shortAnswers = createShortAnswers(questionDto.getShortAnswers(), question);

		questionEntityRepository.save(question);
		shortAnswerEntityRepository.saveAll(shortAnswers);
	}

	@Override
	public QuestionDto getQuestion(QuestionEntity question, boolean answerVisible) {
		List<ShortAnswerEntity> shortAnswers = shortAnswerEntityRepository.findAllByShortQuestionId(
			question.getId());
		return ShortQuestionDto.of((ShortQuestionEntity)question, shortAnswers, answerVisible);
	}

	public ShortQuestionEntity create(ShortQuestionDto dto, QuestionSetEntity questionSetEntity) {
		return ShortQuestionEntity.builder()
			.number(dto.getNumber())
			.questionSet(questionSetEntity)
			.content(dto.getContent())
			.explanation(dto.getExplanation())
			.displayDelayMilliseconds(RandomUtil.getRandomNumber(QuestionConstant.MAX_DISPLAY_DELAY_MILLISECONDS))
			.answerCount(dto.getShortAnswers().size())
			.build();
	}

	public List<ShortAnswerEntity> createShortAnswers(List<ShortAnswerDto> shortAnswerDtos,
		ShortQuestionEntity shortQuestionEntity) {
		validateMainAnswers(shortAnswerDtos);
		return shortAnswerDtos.stream()
			.map(shortAnswerDto -> ShortAnswerEntity.builder()
				.answer(shortAnswerDto.getAnswer())
				.isMain(shortAnswerDto.isMain())
				.number(shortAnswerDto.getNumber())
				.shortQuestionId(shortQuestionEntity.getId())
				.build())
			.toList();
	}

	private void validateMainAnswers(List<ShortAnswerDto> answers) {
		answers.stream()
			.collect(Collectors.groupingBy(ShortAnswerDto::getNumber))
			.forEach(this::validateSingleMainAnswer);
	}

	private void validateSingleMainAnswer(Long number, List<ShortAnswerDto> shortAnswerDtos) {
		long mainCount = shortAnswerDtos.stream()
			.mapToLong(answer -> answer.isMain() ? 1 : 0)
			.sum();

		if (mainCount != 1) {
			throw new UserParameterException(
				String.format(
					"Each short answer number must have exactly one main answer. Number %d has %d main answers.",
					number, mainCount));
		}
	}
}
