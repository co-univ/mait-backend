package com.coniv.mait.domain.question.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;

import jakarta.persistence.LockModeType;

public interface QuestionSetEntityRepository extends JpaRepository<QuestionSetEntity, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select qs from QuestionSetEntity qs where qs.id = :id")
	Optional<QuestionSetEntity> findByIdForUpdate(@Param("id") Long id);

	List<QuestionSetEntity> findAllByTeamId(Long teamId);

	List<QuestionSetEntity> findAllByTeamIdAndStatus(Long teamId, QuestionSetStatus status);

	List<QuestionSetEntity> findAllByTeamIdAndStatusIn(Long teamId, List<QuestionSetStatus> statuses);

	List<QuestionSetEntity> findAllByTeamIdAndSolveModeAndStatusIn(Long teamId, QuestionSetSolveMode solveMode,
		List<QuestionSetStatus> statuses);

	List<QuestionSetEntity> findAllByIdInAndStatusIn(List<Long> ids, List<QuestionSetStatus> statuses);
}
