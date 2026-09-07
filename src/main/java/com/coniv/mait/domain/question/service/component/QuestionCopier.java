package com.coniv.mait.domain.question.service.component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;

@Component
public class QuestionCopier {

	private final QuestionEntityRepository questionEntityRepository;

	private final Map<QuestionType, QuestionFactory<?>> questionFactories;

	public QuestionCopier(final QuestionEntityRepository questionEntityRepository,
		final List<QuestionFactory<?>> questionFactories) {
		this.questionEntityRepository = questionEntityRepository;
		this.questionFactories = questionFactories.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionFactory::getQuestionType, Function.identity()));
	}

	@Transactional
	public void copyQuestions(final Long sourceQuestionSetId, final QuestionSetEntity targetQuestionSet) {
		List<QuestionEntity> sources = questionEntityRepository.findAllByQuestionSetId(sourceQuestionSetId);
		if (sources.isEmpty()) {
			return;
		}

		Map<Long, QuestionEntity> copiedBySourceQuestionId = new LinkedHashMap<>();
		for (QuestionEntity source : sources) {
			QuestionEntity copied = questionFactories.get(source.getType()).copyQuestion(source, targetQuestionSet);
			copiedBySourceQuestionId.put(source.getId(), copied);
		}
		questionEntityRepository.saveAll(copiedBySourceQuestionId.values());

		Map<QuestionType, Map<Long, QuestionEntity>> copiedByQuestionType = copiedBySourceQuestionId.entrySet()
			.stream()
			.collect(Collectors.groupingBy(entry -> entry.getValue().getType(),
				Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

		copiedByQuestionType.forEach((questionType, copiedOfType) ->
			questionFactories.get(questionType).copySubEntities(copiedOfType));
	}
}
