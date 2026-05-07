package com.coniv.mait.domain.question.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.exception.QuestionSetCategoryException;
import com.coniv.mait.domain.question.exception.code.QuestionSetCategoryExceptionCode;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetCategoryDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionSetCategoryService {

	private final QuestionSetCategoryEntityRepository questionSetCategoryEntityRepository;

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

		QuestionSetCategoryEntity category = questionSetCategoryEntityRepository.save(
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
}
