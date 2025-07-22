package com.coniv.mait.domain.question.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.MultipleQuestionFactory;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionService {

	private final QuestionEntityRepository questionEntityRepository;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	private final MultipleQuestionFactory multipleQuestionFactory;

	@Transactional
	public void saveQuestionsToQuestionSet(
		final Long questionSetId,
		final List<MultipleQuestionDto> multipleQuestionsDto
	) {
		QuestionSetEntity questionSetEntity = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));

		multipleQuestionsDto.forEach(multiple -> {
			MultipleQuestionEntity multipleQuestion = multipleQuestionFactory.create(multiple, questionSetEntity);
			List<MultipleChoiceEntity> choices = multipleQuestionFactory.createChoices(multiple.getChoices(),
				multipleQuestion);
			questionEntityRepository.save(multipleQuestion);
			// Todo: JDBC Batch Insert로 변경
			multipleChoiceEntityRepository.saveAll(choices);
		});

		// Todo: 주관식

		// Todo: 순서

		// Todo: 빈칸
	}
}
