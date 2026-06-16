package com.coniv.mait.domain.question.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryLinkEntity;

public interface QuestionSetCategoryLinkEntityRepository
	extends JpaRepository<QuestionSetCategoryLinkEntity, Long> {

	List<QuestionSetCategoryLinkEntity> findAllByQuestionSetId(Long questionSetId);

	List<QuestionSetCategoryLinkEntity> findAllByCategoryId(Long categoryId);

	List<QuestionSetCategoryLinkEntity> findAllByCategoryIdIn(Collection<Long> categoryIds);

	boolean existsByQuestionSetIdAndCategoryId(Long questionSetId, Long categoryId);

	@Modifying(clearAutomatically = true)
	void deleteByQuestionSetIdAndCategoryId(Long questionSetId, Long categoryId);

	@Modifying(clearAutomatically = true)
	void deleteByQuestionSetIdAndCategoryIdIn(Long questionSetId, Collection<Long> categoryIds);
}
