package com.coniv.mait.domain.solve.entity;

import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Table(name = "study_answer_drafts")
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StudyAnswerDraftEntity extends BaseTimeEntity implements Persistable<StudyAnswerDraftId> {

	@EmbeddedId
	private StudyAnswerDraftId id;

	@MapsId("solvingSessionId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "solving_session_id", nullable = false)
	private SolvingSessionEntity solvingSession;

	@Column(columnDefinition = "json")
	private String submittedAnswer;

	@Column(name = "submitted", nullable = false)
	@Builder.Default
	private boolean submitted = false;

	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

	public void updateSubmittedAnswer(String submittedAnswer) {
		this.submittedAnswer = submittedAnswer;
		this.submitted = true;
	}

	public static StudyAnswerDraftEntity of(SolvingSessionEntity solvingSession, Long questionId) {
		StudyAnswerDraftId key = StudyAnswerDraftId.builder()
			.solvingSessionId(solvingSession.getId())
			.questionId(questionId)
			.build();
		return StudyAnswerDraftEntity.builder()
			.id(key)
			.solvingSession(solvingSession)
			.build();
	}
}
