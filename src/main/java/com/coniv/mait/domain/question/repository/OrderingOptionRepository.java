package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.OrderingOptionEntity;

public interface OrderingOptionRepository extends JpaRepository<OrderingOptionEntity, Long> {

	List<OrderingOptionEntity> findAllByOrderingQuestionId(Long orderingQuestionId);
}
