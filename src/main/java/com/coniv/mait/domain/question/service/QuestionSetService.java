package com.coniv.mait.domain.question.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import jakarta.persistence.EntityNotFoundException;
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
			.title(questionSetEntity.getTitle())
			.build();
	}

	public List<QuestionSetDto> getQuestionSets(final Long teamId) {
		// Todo: 조회하려는 유저와 팀이 일치하는지 확인
		return questionSetEntityRepository.findAllByTeamId(teamId).stream()
			.sorted(Comparator.comparing(
				QuestionSetEntity::getCreatedAt,
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

	@Transactional
	public QuestionSetDto completeQuestionSet(
		final Long questionSetId,
		final String title,
		final String subject,
		final DeliveryMode mode,
		final String levelDescription,
		final QuestionSetVisibility visibility
	) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("Question set not found"));

		// Todo:  현재 생성 단계가 아니면 예외
		questionSet.completeQuestionSet(title, subject, mode, levelDescription, visibility);
		return QuestionSetDto.from(questionSet);
	}
}
