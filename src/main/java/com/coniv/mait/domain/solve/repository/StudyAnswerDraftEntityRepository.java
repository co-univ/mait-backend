package com.coniv.mait.domain.solve.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.solve.entity.StudyAnswerDraftEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftId;

public interface StudyAnswerDraftEntityRepository extends JpaRepository<StudyAnswerDraftEntity, StudyAnswerDraftId> {
}
