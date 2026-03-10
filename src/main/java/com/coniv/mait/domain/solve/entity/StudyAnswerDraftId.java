package com.coniv.mait.domain.solve.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyAnswerDraftId implements Serializable {

	@Column(name = "solving_session_id", nullable = false)
	private Long solvingSessionId;

	@Column(name = "question_id", nullable = false)
	private Long questionId;
}
