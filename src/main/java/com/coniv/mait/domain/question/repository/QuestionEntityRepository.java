package com.coniv.mait.domain.question.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionStatusType;

public interface QuestionEntityRepository extends JpaRepository<QuestionEntity, Long> {
	List<QuestionEntity> findAllByQuestionSetId(final Long questionSetId);

	List<QuestionEntity> findAllByQuestionSetIdOrderByLexoRankAsc(final Long questionSetId);

	long countByQuestionSetId(Long questionSetId);

	Optional<QuestionEntity> findFirstByQuestionSetAndQuestionStatusIn(
		QuestionSetEntity questionSet,
		List<QuestionStatusType> statuses
	);

	Optional<QuestionEntity> findTopByQuestionSetIdOrderByLexoRankDesc(Long questionSetId);
}
