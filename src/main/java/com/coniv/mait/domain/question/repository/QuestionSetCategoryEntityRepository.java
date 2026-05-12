package com.coniv.mait.domain.question.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;

public interface QuestionSetCategoryEntityRepository extends JpaRepository<QuestionSetCategoryEntity, Long> {

	boolean existsByTeamIdAndNameAndDeletedAtIsNull(Long teamId, String name);

	boolean existsByTeamIdAndNameAndDeletedAtIsNotNull(Long teamId, String name);

	Optional<QuestionSetCategoryEntity> findByTeamIdAndNameAndDeletedAtIsNotNull(Long teamId, String name);

	List<QuestionSetCategoryEntity> findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long teamId);

	List<QuestionSetCategoryEntity> findAllByTeamIdAndNameContainingAndDeletedAtIsNullOrderByCreatedAtAsc(
		Long teamId, String keyword);

	List<QuestionSetCategoryEntity> findAllByIdInAndTeamIdAndDeletedAtIsNull(Collection<Long> ids, Long teamId);

	List<QuestionSetCategoryEntity> findAllByIdIn(Collection<Long> ids);

	Optional<QuestionSetCategoryEntity> findByIdAndDeletedAtIsNull(Long id);

	Optional<QuestionSetCategoryEntity> findByIdAndTeamIdAndDeletedAtIsNull(Long id, Long teamId);
}
