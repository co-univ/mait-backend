package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;

public interface FillBlankAnswerEntityRepository extends JpaRepository<FillBlankAnswerEntity, Long> {
	List<FillBlankAnswerEntity> findAllByFillBlankQuestionId(Long fillBlankQuestionId);
}
