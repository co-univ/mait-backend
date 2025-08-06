package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;

public interface QuestionSetParticipantRepository extends JpaRepository<QuestionSetParticipantEntity, Long> {
	void deleteAllByQuestionSet(QuestionSetEntity questionSet);

	@Query("SELECT p FROM QuestionSetParticipantEntity p join fetch p.user WHERE p.questionSet = :questionSet")
	List<QuestionSetParticipantEntity> findAllByQuestionSetWithFetchJoinUser(
		@Param("questionSet") QuestionSetEntity questionSet);
}


