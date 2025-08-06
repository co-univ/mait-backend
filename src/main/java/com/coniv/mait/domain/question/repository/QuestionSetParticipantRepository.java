package com.coniv.mait.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;

public interface QuestionSetParticipantRepository extends JpaRepository<QuestionSetParticipantEntity, Long> {
	void deleteAllByQuestionSet(QuestionSetEntity questionSet);
}


