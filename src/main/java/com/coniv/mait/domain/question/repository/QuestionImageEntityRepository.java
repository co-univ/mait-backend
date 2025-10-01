package com.coniv.mait.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionImageEntity;

public interface QuestionImageEntityRepository extends JpaRepository<QuestionImageEntity, Long> {
}
