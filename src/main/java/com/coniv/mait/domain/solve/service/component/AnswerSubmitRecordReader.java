package com.coniv.mait.domain.solve.service.component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnswerSubmitRecordReader {

	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	public Map<Long, AnswerSubmitRecordEntity> getEarliestByQuestionId(final Long userId,
		final List<Long> questionIds) {
		return answerSubmitRecordEntityRepository.findAllByUserIdAndQuestionIdIn(userId, questionIds).stream()
			.collect(Collectors.groupingBy(
				AnswerSubmitRecordEntity::getQuestionId,
				Collectors.collectingAndThen(Collectors.toList(), this::pickEarliest)));
	}

	public AnswerSubmitRecordEntity pickEarliest(final List<AnswerSubmitRecordEntity> records) {
		return records.stream()
			.min(Comparator.comparing(AnswerSubmitRecordEntity::getSubmitOrder,
				Comparator.nullsFirst(Comparator.naturalOrder())))
			.orElseThrow();
	}
}
