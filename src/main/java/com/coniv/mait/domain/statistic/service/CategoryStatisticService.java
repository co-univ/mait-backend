package com.coniv.mait.domain.statistic.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryLinkEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryLinkEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.statistic.service.component.QuestionSetStatisticCalculator;
import com.coniv.mait.domain.statistic.service.dto.CategoryCorrectRateDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryStatisticService {

	/** 내 정답률 높은 순, 미응시(null)는 항상 맨 뒤, 동률이면 카테고리 이름 오름차순. */
	private static final Comparator<CategoryCorrectRateDto> RANK_COMPARATOR =
		Comparator.comparing(CategoryCorrectRateDto::getMyCorrectRate,
				Comparator.nullsLast(Comparator.reverseOrder()))
			.thenComparing(CategoryCorrectRateDto::getCategoryName);

	private final QuestionSetCategoryEntityRepository questionSetCategoryEntityRepository;
	private final QuestionSetCategoryLinkEntityRepository questionSetCategoryLinkEntityRepository;
	private final QuestionSetReader questionSetReader;
	private final QuestionEntityRepository questionEntityRepository;
	private final TeamRoleValidator teamRoleValidator;
	private final QuestionSetStatisticCalculator questionSetStatisticCalculator;

	@Transactional(readOnly = true)
	public CategoryCorrectRateDto getCategoryCorrectRate(final MaitUser user, final Long teamId,
		final Long categoryId) {
		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(teamId, user.id());

		QuestionSetCategoryEntity category = questionSetCategoryEntityRepository
			.findByIdAndTeamIdAndDeletedAtIsNull(categoryId, teamId)
			.orElseThrow(() -> new EntityNotFoundException("해당 카테고리를 찾을 수 없습니다."));

		List<Long> linkedQuestionSetIds = questionSetCategoryLinkEntityRepository.findAllByCategoryId(categoryId)
			.stream()
			.map(QuestionSetCategoryLinkEntity::getQuestionSetId)
			.toList();

		List<Long> finishedQuestionSetIds = questionSetReader.getFinishedQuestionSets(linkedQuestionSetIds)
			.stream()
			.map(QuestionSetEntity::getId)
			.toList();
		if (finishedQuestionSetIds.isEmpty()) {
			return CategoryCorrectRateDto.of(category, 0, null, 0.0);
		}

		List<QuestionEntity> questions = questionEntityRepository.findAllByQuestionSetIdIn(finishedQuestionSetIds);

		Double myCorrectRate = questionSetStatisticCalculator.calculateUserCorrectRate(user.id(), questions);
		double averageCorrectRate = questionSetStatisticCalculator.calculateOverallCorrectRate(questions);

		return CategoryCorrectRateDto.of(category, finishedQuestionSetIds.size(), myCorrectRate, averageCorrectRate);
	}

	@Transactional(readOnly = true)
	public List<CategoryCorrectRateDto> getCategoryCorrectRates(final MaitUser user, final Long teamId) {
		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(teamId, user.id());

		List<QuestionSetCategoryEntity> categories =
			questionSetCategoryEntityRepository.findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(teamId);
		if (categories.isEmpty()) {
			return List.of();
		}

		Map<Long, List<QuestionSetEntity>> finishedSetsByCategoryId =
			questionSetReader.getFinishedQuestionSetsByCategoryId(categories);
		List<Long> finishedQuestionSetIds = finishedSetsByCategoryId.values().stream()
			.flatMap(List::stream)
			.map(QuestionSetEntity::getId)
			.distinct()
			.toList();
		Map<Long, List<QuestionEntity>> questionsByQuestionSetId =
			questionSetReader.getQuestionsByQuestionSetId(finishedQuestionSetIds);

		Map<Long, List<QuestionEntity>> questionsByCategoryId = new HashMap<>();
		Map<Long, Integer> finishedQuestionSetCountByCategoryId = new HashMap<>();

		for (QuestionSetCategoryEntity category : categories) {
			List<QuestionSetEntity> finishedSets = finishedSetsByCategoryId.getOrDefault(category.getId(), List.of());
			List<QuestionEntity> questions = finishedSets.stream()
				.flatMap(questionSet ->
					questionsByQuestionSetId.getOrDefault(questionSet.getId(), List.of()).stream())
				.toList();
			questionsByCategoryId.put(category.getId(), questions);
			finishedQuestionSetCountByCategoryId.put(category.getId(), finishedSets.size());
		}

		Map<Long, Double> myRates = questionSetStatisticCalculator.calculateUserCorrectRates(user.id(),
			questionsByCategoryId);
		Map<Long, Double> averageRates = questionSetStatisticCalculator.calculateOverallCorrectRates(
			questionsByCategoryId);

		return categories.stream()
			.map(category -> CategoryCorrectRateDto.of(category,
				finishedQuestionSetCountByCategoryId.getOrDefault(category.getId(), 0),
				myRates.get(category.getId()),
				averageRates.getOrDefault(category.getId(), 0.0)))
			.sorted(RANK_COMPARATOR)
			.toList();
	}
}
