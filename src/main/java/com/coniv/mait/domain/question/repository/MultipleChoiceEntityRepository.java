package com.coniv.mait.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;

public interface MultipleChoiceEntityRepository extends JpaRepository<MultipleChoiceEntity, Long> {
}
