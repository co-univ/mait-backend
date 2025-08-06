package com.coniv.mait.domain.solve.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;

public interface QuestionScorerEntityRepository extends JpaRepository<QuestionScorerEntity, Long> {
	Optional<QuestionScorerEntity> findByQuestionId(Long questionId);
}
