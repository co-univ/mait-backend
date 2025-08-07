package com.coniv.mait.domain.solve.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;

public interface AnswerSubmitRecordEntityRepository extends JpaRepository<AnswerSubmitRecordEntity, Long> {
	List<AnswerSubmitRecordEntity> findAllByQuestionId(Long questionId);

	List<AnswerSubmitRecordEntity> findAllByQuestionIdInAndIsCorrect(List<Long> questionIds, boolean isCorrect);

	boolean existsByUserIdAndQuestionIdAndIsCorrectTrue(Long id, Long questionId);
}
