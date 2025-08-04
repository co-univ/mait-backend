package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionEntity;

public interface QuestionEntityRepository extends JpaRepository<QuestionEntity, Long> {
	List<QuestionEntity> findAllByQuestionSetId(final Long questionSetId);
}
