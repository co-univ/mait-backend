package com.coniv.mait.domain.question.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;

public interface QuestionSetParticipantRepository extends JpaRepository<QuestionSetParticipantEntity, Long> {
	void deleteAllByQuestionSet(QuestionSetEntity questionSet);

	@Modifying
	@Query("DELETE FROM QuestionSetParticipantEntity p WHERE p.questionSet.id = :questionSetId")
	void deleteAllByQuestionSetId(@Param("questionSetId") Long questionSetId);

	@Query("SELECT p FROM QuestionSetParticipantEntity p join fetch p.user WHERE p.questionSet = :questionSet")
	List<QuestionSetParticipantEntity> findAllByQuestionSetWithFetchJoinUser(
		@Param("questionSet") QuestionSetEntity questionSet);

	@Query("SELECT p FROM QuestionSetParticipantEntity p JOIN FETCH p.questionSet "
		+ "WHERE p.user.id = :userId AND p.questionSet.teamId = :teamId")
	List<QuestionSetParticipantEntity> findAllByUserIdAndQuestionSetTeamId(
		@Param("userId") Long userId,
		@Param("teamId") Long teamId);

	List<QuestionSetParticipantEntity> findAllByQuestionSetId(Long questionSetId);

	boolean existsByQuestionSetIdAndUserIdAndStatus(Long questionSetId, Long userId, ParticipantStatus status);

	boolean existsByQuestionSetAndUserId(QuestionSetEntity questionSet, Long userId);

	Optional<QuestionSetParticipantEntity> findByQuestionSetAndUserId(QuestionSetEntity questionSet, Long userId);
}
