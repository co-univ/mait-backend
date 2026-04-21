package com.coniv.mait.domain.solve.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;

public interface AnswerSubmitRecordEntityRepository extends JpaRepository<AnswerSubmitRecordEntity, Long> {
	List<AnswerSubmitRecordEntity> findAllByQuestionId(Long questionId);

	List<AnswerSubmitRecordEntity> findAllByQuestionIdIn(List<Long> questionIds);

	List<AnswerSubmitRecordEntity> findAllByQuestionIdInAndIsCorrect(List<Long> questionIds, boolean isCorrect);

	boolean existsByUserIdAndQuestionIdAndIsCorrectTrue(Long id, Long questionId);

	boolean existsByQuestionIdIn(List<Long> questionIds);

	List<AnswerSubmitRecordEntity> findAllByUserIdAndQuestionIdIn(Long userId, List<Long> questionIds);

	@Modifying
	@Query("DELETE FROM AnswerSubmitRecordEntity a WHERE a.questionId IN :questionIds")
	void deleteAllByQuestionIdIn(@Param("questionIds") List<Long> questionIds);
}
