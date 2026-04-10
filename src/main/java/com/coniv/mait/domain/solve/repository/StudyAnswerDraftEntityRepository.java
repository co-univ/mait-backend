package com.coniv.mait.domain.solve.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coniv.mait.domain.solve.entity.StudyAnswerDraftEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftId;

public interface StudyAnswerDraftEntityRepository extends JpaRepository<StudyAnswerDraftEntity, StudyAnswerDraftId> {
	List<StudyAnswerDraftEntity> findAllByIdSolvingSessionIdOrderByIdQuestionIdAsc(Long solvingSessionId);

	@Modifying
	@Query("DELETE FROM StudyAnswerDraftEntity d WHERE d.id.solvingSessionId IN :sessionIds")
	void deleteAllBySolvingSessionIdIn(@Param("sessionIds") List<Long> sessionIds);
}
