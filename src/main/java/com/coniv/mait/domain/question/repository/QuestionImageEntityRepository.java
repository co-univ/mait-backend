package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionImageEntity;

public interface QuestionImageEntityRepository extends JpaRepository<QuestionImageEntity, Long> {
	List<QuestionImageEntity> findAllByQuestionAndUsedIsTrue(QuestionEntity question);
}
