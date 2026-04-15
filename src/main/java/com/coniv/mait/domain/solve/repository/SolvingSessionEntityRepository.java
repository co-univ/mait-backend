package com.coniv.mait.domain.solve.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;

public interface SolvingSessionEntityRepository extends JpaRepository<SolvingSessionEntity, Long> {

	Optional<SolvingSessionEntity> findByUserIdAndQuestionSetIdAndMode(Long userId, Long questionSetId,
		DeliveryMode deliveryMode);

	List<SolvingSessionEntity> findAllByUserIdAndStatusAndModeAndQuestionSetTeamId(Long userId, SolvingStatus status,
		DeliveryMode mode, Long teamId);

	List<SolvingSessionEntity> findAllByUserIdAndModeAndQuestionSetTeamId(Long userId, DeliveryMode mode, Long teamId);

	@Query("SELECT s.id FROM SolvingSessionEntity s WHERE s.questionSet.id = :questionSetId")
	List<Long> findSessionIdsByQuestionSetId(@Param("questionSetId") Long questionSetId);

	@Modifying
	@Query("DELETE FROM SolvingSessionEntity s WHERE s.questionSet.id = :questionSetId")
	void deleteAllByQuestionSetId(@Param("questionSetId") Long questionSetId);
}
