package com.coniv.mait.domain.solve.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;

public interface SolvingSessionEntityRepository extends JpaRepository<SolvingSessionEntity, Long> {

	Optional<SolvingSessionEntity> findByUserIdAndQuestionSetIdAndMode(Long userId, Long questionSetId,
		DeliveryMode deliveryMode);

	List<SolvingSessionEntity> findAllByUserIdAndModeAndQuestionSetTeamId(Long userId, DeliveryMode mode, Long teamId);

	List<SolvingSessionEntity> findAllByUserIdAndStatusAndModeAndQuestionSetTeamId(Long userId, SolvingStatus status,
		DeliveryMode mode, Long teamId);

	boolean existsByQuestionSetId(Long questionSetId);
}
