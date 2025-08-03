package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;

public interface QuestionSetEntityRepository extends JpaRepository<QuestionSetEntity, Long> {
	List<QuestionSetEntity> findAllByTeamId(Long teamId);
}
