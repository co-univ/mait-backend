package com.coniv.mait.domain.solve.service.component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuestionParticipantReader {

	private final QuestionSetParticipantRepository questionSetParticipantRepository;

	public Map<Long, List<QuestionSetParticipantEntity>> getWinnersByQuestionSetId(final List<Long> questionSetIds) {
		if (questionSetIds.isEmpty()) {
			return Map.of();
		}
		return questionSetParticipantRepository.findAllByQuestionSetIdIn(questionSetIds).stream()
			.filter(QuestionSetParticipantEntity::isWinner)
			.collect(Collectors.groupingBy(participant -> participant.getQuestionSet().getId()));
	}
}
