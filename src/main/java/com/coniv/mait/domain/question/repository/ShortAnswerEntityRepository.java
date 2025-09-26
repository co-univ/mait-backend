package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.ShortAnswerEntity;

public interface ShortAnswerEntityRepository extends JpaRepository<ShortAnswerEntity, Long> {
	List<ShortAnswerEntity> findAllByShortQuestionId(Long shortQuestionId);

	void deleteAllByShortQuestionId(Long questionId);
}
