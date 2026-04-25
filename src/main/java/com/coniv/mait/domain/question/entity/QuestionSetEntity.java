package com.coniv.mait.domain.question.entity;

import java.time.LocalDateTime;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.global.entity.BaseTimeEntity;
import com.coniv.mait.global.exception.custom.QuestionSetLiveException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "question_sets")
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionSetEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String subject;

	@Builder.Default
	private String title = "문제 셋";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private QuestionSetCreationType creationType = QuestionSetCreationType.MANUAL;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private QuestionSetVisibility visibility = QuestionSetVisibility.GROUP;

	@Enumerated(EnumType.STRING)
	@Column
	private QuestionSetSolveMode solveMode;

	// @Column(nullable = false)
	private Long teamId;

	private Long creatorId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	@Builder.Default
	private QuestionSetStatus status = QuestionSetStatus.BEFORE;

	private String difficulty;

	@Builder.Default
	private boolean advancementSelected = false;

	private LocalDateTime startTime;

	private LocalDateTime endTime;

	private QuestionSetEntity(String subject, QuestionSetCreationType creationType) {
		this.subject = subject;
		this.creationType = creationType;
	}

	public static QuestionSetEntity of(String subject, QuestionSetCreationType creationType) {
		return QuestionSetEntity.builder()
			.subject(subject)
			.creationType(creationType)
			.build();
	}

	public boolean isOnLive() {
		return status == QuestionSetStatus.ONGOING && solveMode == QuestionSetSolveMode.LIVE_TIME;
	}

	public void startLiveQuestionSet() {
		checkLiveTimeMode();
		if (status != QuestionSetStatus.BEFORE) {
			throw new QuestionSetLiveException("BEFORE_LIVE 상태에서만 실시간 문제를 시작할 수 있습니다. 현재 상태: " + status);
		}
		this.status = QuestionSetStatus.ONGOING;
		this.startTime = LocalDateTime.now();
	}

	public void endLiveQuestionSet() {
		checkLiveTimeMode();
		if (status != QuestionSetStatus.ONGOING) {
			throw new QuestionSetLiveException("LIVE 상태에서만 실시간 문제를 종료할 수 있습니다. 현재 상태: " + status);
		}
		this.status = QuestionSetStatus.AFTER;
		this.endTime = LocalDateTime.now();
	}

	private void checkLiveTimeMode() {
		if (solveMode != QuestionSetSolveMode.LIVE_TIME) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_LIVE_TIME);
		}
	}

	public void startStudyQuestionSet() {
		checkStudyDeliveryMode();
		if (status != QuestionSetStatus.BEFORE) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_BEFORE);
		}
		this.status = QuestionSetStatus.ONGOING;
		this.startTime = LocalDateTime.now();
	}

	private void checkStudyDeliveryMode() {
		if (solveMode != QuestionSetSolveMode.STUDY) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_STUDY);
		}
	}

	public void completeQuestionSet(String title, String subject, QuestionSetSolveMode solveMode, String difficulty,
		QuestionSetVisibility visibility) {
		if (solveMode == null) {
			throw new IllegalArgumentException("문제 셋 완료 시 solveMode는 필수입니다.");
		}

		this.title = title;
		this.subject = subject;
		this.solveMode = solveMode;
		this.status = QuestionSetStatus.BEFORE;
		this.difficulty = difficulty;
		this.visibility = visibility;
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void openReview(QuestionSetVisibility visibility) {
		if (status != QuestionSetStatus.AFTER) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_AFTER);
		}
		this.status = QuestionSetStatus.REVIEW;
		this.visibility = visibility;
	}

	public boolean canReview() {
		return status == QuestionSetStatus.REVIEW;
	}

	public void restartLive() {
		if (status != QuestionSetStatus.AFTER) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_AFTER);
		}
		if (solveMode != QuestionSetSolveMode.LIVE_TIME) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_LIVE_TIME);
		}
		this.status = QuestionSetStatus.ONGOING;
	}

	public void markAdvancementSelected() {
		this.advancementSelected = true;
	}

	public DeliveryMode getDisplayMode() {
		if (canReview()) {
			return DeliveryMode.REVIEW;
		}

		if (solveMode == null) {
			return DeliveryMode.MAKING;
		}

		return DeliveryMode.from(solveMode);
	}
}
