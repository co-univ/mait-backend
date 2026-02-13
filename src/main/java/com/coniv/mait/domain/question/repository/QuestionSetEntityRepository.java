package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;

public interface QuestionSetEntityRepository extends JpaRepository<QuestionSetEntity, Long> {
	List<QuestionSetEntity> findAllByTeamId(Long teamId);

	List<QuestionSetEntity> findAllByTeamIdAndDeliveryMode(Long teamId, DeliveryMode deliveryMode);

	List<QuestionSetEntity> findAllByTeamIdAndOngoingStatus(Long teamId, QuestionSetOngoingStatus status);
}
