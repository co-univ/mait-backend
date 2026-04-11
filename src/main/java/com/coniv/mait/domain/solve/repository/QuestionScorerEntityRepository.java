package com.coniv.mait.domain.solve.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;

public interface QuestionScorerEntityRepository extends JpaRepository<QuestionScorerEntity, Long> {
	Optional<QuestionScorerEntity> findByQuestionId(Long questionId);

	List<QuestionScorerEntity> findAllByQuestionIdIn(List<Long> questionIds);

	@Modifying
	@Query("DELETE FROM QuestionScorerEntity s WHERE s.questionId IN :questionIds")
	void deleteAllByQuestionIdIn(@Param("questionIds") List<Long> questionIds);
}
