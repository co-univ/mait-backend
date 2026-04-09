package com.coniv.mait.domain.question.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;

public interface QuestionSetEntityRepository extends JpaRepository<QuestionSetEntity, Long> {
	List<QuestionSetEntity> findAllByTeamId(Long teamId);

	List<QuestionSetEntity> findAllByTeamIdAndStatus(Long teamId, QuestionSetStatus status);

	List<QuestionSetEntity> findAllByTeamIdAndStatusIn(Long teamId, List<QuestionSetStatus> statuses);

	List<QuestionSetEntity> findAllByTeamIdAndSolveModeAndStatusIn(Long teamId, QuestionSetSolveMode solveMode,
		List<QuestionSetStatus> statuses);

	List<QuestionSetEntity> findAllBySolveModeIsNullAndDeliveryModeIn(List<DeliveryMode> deliveryModes);
}
