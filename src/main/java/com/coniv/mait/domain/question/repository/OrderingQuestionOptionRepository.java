package com.coniv.mait.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;

public interface OrderingQuestionOptionRepository extends JpaRepository<OrderingOptionEntity, Long> {

}
