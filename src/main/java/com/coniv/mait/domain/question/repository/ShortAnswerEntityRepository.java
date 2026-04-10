package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.question.entity.ShortAnswerEntity;

public interface ShortAnswerEntityRepository extends JpaRepository<ShortAnswerEntity, Long> {
	List<ShortAnswerEntity> findAllByShortQuestionId(Long shortQuestionId);

	void deleteAllByShortQuestionId(Long questionId);

	@Modifying
	@Query("DELETE FROM ShortAnswerEntity s WHERE s.shortQuestionId IN :questionIds")
	void deleteAllByShortQuestionIdIn(@Param("questionIds") List<Long> questionIds);

	int countByShortQuestionIdAndIsMainTrue(Long shortQuestionId);
}
