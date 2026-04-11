package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;

public interface MultipleChoiceEntityRepository extends JpaRepository<MultipleChoiceEntity, Long> {
	List<MultipleChoiceEntity> findAllByQuestionId(Long questionId);

	@Modifying
	@Query("DELETE FROM MultipleChoiceEntity m WHERE m.question.id = :questionId")
	void deleteBulkAllByQuestionId(@Param("questionId") Long questionId);

	@Modifying
	@Query("DELETE FROM MultipleChoiceEntity m WHERE m.question.id IN :questionIds")
	void deleteAllByQuestionIdIn(@Param("questionIds") List<Long> questionIds);
}
