package com.coniv.mait.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.ShortAnswerEntity;

public interface ShortAnswerEntityRepository extends JpaRepository<ShortAnswerEntity, Long> {
}
