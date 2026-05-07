package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;

public interface QuestionSetCategoryEntityRepository extends JpaRepository<QuestionSetCategoryEntity, Long> {

	boolean existsByTeamIdAndNameAndDeletedAtIsNull(Long teamId, String name);

	boolean existsByTeamIdAndNameAndDeletedAtIsNotNull(Long teamId, String name);

	List<QuestionSetCategoryEntity> findAllByTeamIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long teamId);
}
