package com.coniv.mait.domain.question.service.component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryLinkEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryLinkEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuestionSetReader {

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionEntityRepository questionEntityRepository;
	private final QuestionSetCategoryLinkEntityRepository questionSetCategoryLinkEntityRepository;

	public QuestionSetEntity getQuestionSet(final Long questionSetId) {
		return questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException(questionSetId + " : 해당 문제 셋을 찾을 수 없습니다."));
	}

	public List<QuestionSetEntity> getFinishedLiveQuestionSetsBySolveModeInTeam(final Long teamId,
		final QuestionSetSolveMode solveMode) {
		return questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(teamId,
			solveMode, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW));
	}

	public List<QuestionSetEntity> getFinishedQuestionSets(final List<Long> questionSetIds) {
		if (questionSetIds.isEmpty()) {
			return List.of();
		}
		return questionSetEntityRepository.findAllByIdInAndStatusIn(questionSetIds,
			List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW));
	}

	public Map<Long, List<QuestionSetEntity>> getFinishedQuestionSetsByCategoryId(
		final List<QuestionSetCategoryEntity> categories) {
		List<Long> categoryIds = categories.stream().map(QuestionSetCategoryEntity::getId).toList();
		if (categoryIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, List<Long>> questionSetIdsByCategoryId =
			questionSetCategoryLinkEntityRepository.findAllByCategoryIdIn(categoryIds).stream()
				.collect(Collectors.groupingBy(QuestionSetCategoryLinkEntity::getCategoryId,
					Collectors.mapping(QuestionSetCategoryLinkEntity::getQuestionSetId, Collectors.toList())));

		List<Long> allQuestionSetIds = questionSetIdsByCategoryId.values().stream()
			.flatMap(List::stream)
			.distinct()
			.toList();
		Map<Long, QuestionSetEntity> finishedById = getFinishedQuestionSets(allQuestionSetIds).stream()
			.collect(Collectors.toUnmodifiableMap(QuestionSetEntity::getId, Function.identity()));

		return questionSetIdsByCategoryId.entrySet().stream()
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().stream()
				.map(finishedById::get)
				.filter(Objects::nonNull)
				.toList()));
	}

	public Map<Long, List<QuestionEntity>> getQuestionsByQuestionSetId(final Collection<Long> questionSetIds) {
		if (questionSetIds.isEmpty()) {
			return Map.of();
		}
		return questionEntityRepository.findAllByQuestionSetIdIn(questionSetIds.stream().toList()).stream()
			.collect(Collectors.groupingBy(question -> question.getQuestionSet().getId()));
	}
}
