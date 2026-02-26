package com.coniv.mait.domain.solve.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;

public interface SolvingSessionEntityRepository extends JpaRepository<SolvingSessionEntity, Long> {

	Optional<SolvingSessionEntity> findByUserIdAndQuestionSetIdAndMode(Long id, Long id1, DeliveryMode deliveryMode);
}
