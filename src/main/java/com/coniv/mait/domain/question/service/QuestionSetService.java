package com.coniv.mait.domain.question.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionSetService {

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final QuestionEntityRepository questionEntityRepository;

	@Transactional
	public QuestionSetDto createQuestionSet(final String subject, final QuestionSetCreationType creationType) {
		// Todo: AI 제작인 경우에 분기 필요

		QuestionSetEntity questionSetEntity = QuestionSetEntity.of(subject, creationType);
		questionSetEntityRepository.save(questionSetEntity);

		return QuestionSetDto.builder()
			.id(questionSetEntity.getId())
			.subject(questionSetEntity.getSubject())
			.build();
	}

	public List<QuestionSetDto> getQuestionSets(final Long teamId, final DeliveryMode mode) {
		// Todo: 조회하려는 유저와 팀이 일치하는지 확인

		return questionSetEntityRepository.findAllByTeamIdAndDeliveryMode(teamId, mode).stream()
			.sorted(Comparator.comparing(
				QuestionSetEntity::getModifiedAt,
				Comparator.nullsLast(Comparator.naturalOrder())
			).reversed())
			.map(QuestionSetDto::from)
			.toList();
	}

	public QuestionSetDto getQuestionSet(final Long questionSetId) {
		final QuestionSetEntity questionSetEntity = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new IllegalArgumentException("Question set not found"));

		long questionCount = questionEntityRepository.countByQuestionSetId(questionSetEntity.getId());

		return QuestionSetDto.of(questionSetEntity, questionCount);
	}
}
