package com.coniv.mait.domain.question.entity;

import java.time.LocalDateTime;
import java.util.EnumSet;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
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
	@Column(nullable = false)
	@Builder.Default
	private DeliveryMode deliveryMode = DeliveryMode.MAKING;

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
		checkLiveDeliveryMode();
		if (status != QuestionSetStatus.BEFORE) {
			throw new QuestionSetLiveException("BEFORE_LIVE 상태에서만 실시간 문제를 시작할 수 있습니다. 현재 상태: " + status);
		}
		this.status = QuestionSetStatus.ONGOING;
		this.startTime = LocalDateTime.now();
	}

	public void endLiveQuestionSet() {
		checkLiveDeliveryMode();
		if (status != QuestionSetStatus.ONGOING) {
			throw new QuestionSetLiveException("LIVE 상태에서만 실시간 문제를 종료할 수 있습니다. 현재 상태: " + status);
		}
		this.status = QuestionSetStatus.AFTER;
		this.endTime = LocalDateTime.now();
	}

	private void checkLiveDeliveryMode() {
		if (solveMode != QuestionSetSolveMode.LIVE_TIME) {
			throw new QuestionSetLiveException("LIVE_TIME 모드가 아닌 문제셋은 실시간 시작할 수 없습니다. 현재 solveMode: " + solveMode);
		}
	}

	public void completeQuestionSet(String title, String subject, DeliveryMode mode, String difficulty,
		QuestionSetVisibility visibility) {
		QuestionSetSolveMode solveMode = QuestionSetSolveMode.fromDeliveryMode(mode);
		if (solveMode == null) {
			throw new IllegalArgumentException("문제 셋 완료 시 LIVE_TIME 또는 STUDY만 설정할 수 있습니다. 현재 mode: " + mode);
		}

		this.title = title;
		this.subject = subject;
		this.deliveryMode = solveMode.toDeliveryMode();
		this.solveMode = solveMode;
		this.status = solveMode == QuestionSetSolveMode.STUDY
			? QuestionSetStatus.ONGOING
			: QuestionSetStatus.BEFORE;
		this.difficulty = difficulty;
		this.visibility = visibility;
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void backfillSolveMode(QuestionSetSolveMode solveMode) {
		this.solveMode = solveMode;
	}

	public void openReview(QuestionSetVisibility visibility) {
		validateAfterStatus();
		this.status = QuestionSetStatus.REVIEW;
		this.visibility = visibility;
	}

	public boolean canReview() {
		return status == QuestionSetStatus.REVIEW;
	}

	public void restartLive() {
		validateRestartableStatus();
		if (solveMode != QuestionSetSolveMode.LIVE_TIME) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_LIVE_TIME);
		}
		this.status = QuestionSetStatus.ONGOING;
	}

	public void migrateLegacyReviewState(QuestionSetSolveMode solveMode) {
		this.deliveryMode = solveMode.toDeliveryMode();
		this.solveMode = solveMode;
		this.status = QuestionSetStatus.REVIEW;
	}

	public void markAdvancementSelected() {
		this.advancementSelected = true;
	}

	public DeliveryMode getDisplayMode() {
		if (canReview()) {
			return DeliveryMode.REVIEW;
		}

		return deliveryMode;
	}

	private void validateAfterStatus() {
		if (status != QuestionSetStatus.AFTER) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_AFTER);
		}
	}

	private void validateRestartableStatus() {
		if (!EnumSet.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW).contains(status)) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_AFTER);
		}
	}
}
