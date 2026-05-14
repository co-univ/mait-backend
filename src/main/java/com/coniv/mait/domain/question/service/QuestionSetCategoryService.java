package com.coniv.mait.domain.question.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryLinkEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.exception.QuestionSetCategoryException;
import com.coniv.mait.domain.question.exception.code.QuestionSetCategoryExceptionCode;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryLinkEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetCategoryDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionSetCategoryService {

	private final QuestionSetCategoryEntityRepository questionSetCategoryEntityRepository;

	private final QuestionSetCategoryLinkEntityRepository questionSetCategoryLinkEntityRepository;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final TeamRoleValidator teamRoleValidator;

	@Transactional
	public QuestionSetCategoryDto createCategory(final Long teamId, final String name, final Long userId) {
		teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId);

		if (questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, name)) {
			throw new QuestionSetCategoryException(QuestionSetCategoryExceptionCode.DUPLICATE_NAME);
		}

		if (questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNotNull(teamId, name)) {
			throw new QuestionSetCategoryException(QuestionSetCategoryExceptionCode.DUPLICATE_NAME_DELETED);
		}

		QuestionSetCategoryEntity category = questionSetCategoryEntityRepository.saveAndFlush(
			QuestionSetCategoryEntity.of(teamId, name));
		return QuestionSetCategoryDto.from(category);
	}

	public List<QuestionSetCategoryDto> getCategories(final Long teamId, final Long userId) {
		teamRoleValidator.checkIsTeamMember(teamId, userId);

		return questionSetCategoryEntityRepository.findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(teamId)
			.stream()
			.map(QuestionSetCategoryDto::from)
			.toList();
	}

	public List<QuestionSetCategoryDto> searchCategories(final Long teamId, final Long userId, final String keyword) {
		teamRoleValidator.checkIsTeamMember(teamId, userId);

		String trimmedKeyword = keyword == null ? "" : keyword.trim();
		if (trimmedKeyword.isBlank()) {
			return List.of();
		}

		return questionSetCategoryEntityRepository
			.findAllByTeamIdAndNameContainingAndDeletedAtIsNullOrderByCreatedAtAsc(teamId, trimmedKeyword).stream()
			.map(QuestionSetCategoryDto::from)
			.toList();
	}

	@Transactional
	public QuestionSetCategoryDto updateCategoryName(final Long categoryId, final String name, final Long userId) {
		QuestionSetCategoryEntity category = questionSetCategoryEntityRepository.findByIdAndDeletedAtIsNull(categoryId)
			.orElseThrow(() -> new EntityNotFoundException("해당 카테고리를 찾을 수 없습니다."));

		teamRoleValidator.checkHasCreateQuestionSetAuthority(category.getTeamId(), userId);

		if (category.getName().equals(name)) {
			return QuestionSetCategoryDto.from(category);
		}

		if (questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(category.getTeamId(), name)) {
			throw new QuestionSetCategoryException(QuestionSetCategoryExceptionCode.DUPLICATE_NAME);
		}

		if (questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNotNull(
			category.getTeamId(), name)) {
			throw new QuestionSetCategoryException(QuestionSetCategoryExceptionCode.DUPLICATE_NAME_DELETED);
		}

		category.updateName(name);
		questionSetCategoryEntityRepository.saveAndFlush(category);
		return QuestionSetCategoryDto.from(category);
	}

	@Transactional
	public void deleteCategory(final Long categoryId, final Long userId) {
		QuestionSetCategoryEntity category = questionSetCategoryEntityRepository.findById(categoryId)
			.orElseThrow(() -> new EntityNotFoundException("해당 카테고리를 찾을 수 없습니다."));

		teamRoleValidator.checkHasCreateQuestionSetAuthority(category.getTeamId(), userId);

		if (category.deleted()) {
			return;
		}

		category.markDeleted();
	}

	@Transactional
	public QuestionSetCategoryDto restoreCategory(final Long teamId, final String name, final Long userId) {
		teamRoleValidator.checkHasCreateQuestionSetAuthority(teamId, userId);

		if (questionSetCategoryEntityRepository.existsByTeamIdAndNameAndDeletedAtIsNull(teamId, name)) {
			throw new QuestionSetCategoryException(QuestionSetCategoryExceptionCode.ALREADY_ACTIVE);
		}

		QuestionSetCategoryEntity category = questionSetCategoryEntityRepository
			.findByTeamIdAndNameAndDeletedAtIsNotNull(teamId, name)
			.orElseThrow(() -> new EntityNotFoundException("복구할 카테고리를 찾을 수 없습니다."));

		category.restore();
		return QuestionSetCategoryDto.from(category);
	}

	@Transactional
	public void attachCategory(final Long questionSetId, final Long categoryId, final Long userId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("문제 셋을 찾을 수 없습니다."));

		teamRoleValidator.checkHasCreateQuestionSetAuthority(questionSet.getTeamId(), userId);

		if (questionSetCategoryLinkEntityRepository.existsByQuestionSetIdAndCategoryId(questionSetId, categoryId)) {
			return;
		}

		questionSetCategoryEntityRepository.findByIdAndTeamIdAndDeletedAtIsNull(categoryId, questionSet.getTeamId())
			.orElseThrow(() -> new EntityNotFoundException("해당 카테고리를 찾을 수 없습니다."));

		questionSetCategoryLinkEntityRepository.save(QuestionSetCategoryLinkEntity.of(questionSetId, categoryId));
	}

	@Transactional
	public void detachCategory(final Long questionSetId, final Long categoryId, final Long userId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("문제 셋을 찾을 수 없습니다."));

		teamRoleValidator.checkHasCreateQuestionSetAuthority(questionSet.getTeamId(), userId);

		questionSetCategoryLinkEntityRepository.deleteByQuestionSetIdAndCategoryId(questionSetId, categoryId);
	}

	@Transactional
	public void attachCategories(final Long questionSetId, final Long teamId, final Collection<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return;
		}

		Set<Long> uniqueIds = Set.copyOf(categoryIds);

		List<QuestionSetCategoryEntity> categories =
			questionSetCategoryEntityRepository.findAllByIdInAndTeamIdAndDeletedAtIsNull(uniqueIds, teamId);

		if (categories.size() != uniqueIds.size()) {
			throw new QuestionSetCategoryException(QuestionSetCategoryExceptionCode.INVALID_TEAM_OR_NOT_FOUND);
		}

		List<QuestionSetCategoryLinkEntity> links = uniqueIds.stream()
			.map(catId -> QuestionSetCategoryLinkEntity.of(questionSetId, catId))
			.toList();
		questionSetCategoryLinkEntityRepository.saveAll(links);
	}

	@Transactional
	public void updateLinkedCategories(final Long questionSetId, final Long teamId, final List<Long> categoryIds) {
		Set<Long> desired = Set.copyOf(categoryIds);

		Set<Long> existing = questionSetCategoryLinkEntityRepository.findAllByQuestionSetId(questionSetId).stream()
			.map(QuestionSetCategoryLinkEntity::getCategoryId)
			.collect(Collectors.toSet());

		Set<Long> toRemove = SetUtils.difference(existing, desired);
		Set<Long> toAdd = SetUtils.difference(desired, existing);

		attachCategories(questionSetId, teamId, toAdd);
		questionSetCategoryLinkEntityRepository.deleteByQuestionSetIdAndCategoryIdIn(questionSetId, toRemove);
	}

	public List<QuestionSetCategoryDto> getCategoriesByQuestionSetId(final Long questionSetId) {
		List<QuestionSetCategoryLinkEntity> links =
			questionSetCategoryLinkEntityRepository.findAllByQuestionSetId(questionSetId);
		if (links.isEmpty()) {
			return List.of();
		}

		Set<Long> categoryIds = links.stream()
			.map(QuestionSetCategoryLinkEntity::getCategoryId)
			.collect(Collectors.toSet());

		Map<Long, QuestionSetCategoryEntity> categoryById = questionSetCategoryEntityRepository.findAllByIdIn(
				categoryIds).stream()
			.collect(Collectors.toUnmodifiableMap(QuestionSetCategoryEntity::getId, Function.identity()));

		return links.stream()
			.map(link -> QuestionSetCategoryDto.from(categoryById.get(link.getCategoryId())))
			.toList();
	}
}
