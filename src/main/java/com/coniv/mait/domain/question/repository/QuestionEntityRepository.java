package com.coniv.mait.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionEntity;

public interface QuestionEntityRepository extends JpaRepository<QuestionEntity, Long> {
}
