package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;

public interface FillBlankAnswerEntityRepository extends JpaRepository<FillBlankAnswerEntity, Long> {
	List<FillBlankAnswerEntity> findAllByFillBlankQuestionId(Long fillBlankQuestionId);

	@Modifying
	@Query("DELETE FROM FillBlankAnswerEntity f WHERE f.fillBlankQuestionId = :questionId")
	void deleteBulkAllByQuestionId(@Param("questionId") Long questionId);

	@Modifying
	@Query("DELETE FROM FillBlankAnswerEntity f WHERE f.fillBlankQuestionId IN :questionIds")
	void deleteAllByFillBlankQuestionIdIn(@Param("questionIds") List<Long> questionIds);
}
