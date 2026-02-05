package com.coniv.mait.domain.question.entity;

import java.time.LocalDateTime;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
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

	// @Column(nullable = false)
	private Long teamId;

	private Long creatorId;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	private QuestionSetOngoingStatus ongoingStatus = QuestionSetOngoingStatus.BEFORE;

	private String difficulty;

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
		return ongoingStatus == QuestionSetOngoingStatus.ONGOING && deliveryMode == DeliveryMode.LIVE_TIME;
	}

	public void startLiveQuestionSet() {
		checkLiveDeliveryMode();
		if (ongoingStatus != QuestionSetOngoingStatus.BEFORE) {
			throw new QuestionSetLiveException("BEFORE_LIVE 상태에서만 실시간 문제를 시작할 수 있습니다. 현재 상태: " + ongoingStatus);
		}
		this.ongoingStatus = QuestionSetOngoingStatus.ONGOING;
		this.startTime = LocalDateTime.now();
	}

	public void endLiveQuestionSet() {
		checkLiveDeliveryMode();
		if (ongoingStatus != QuestionSetOngoingStatus.ONGOING) {
			throw new QuestionSetLiveException("LIVE 상태에서만 실시간 문제를 종료할 수 있습니다. 현재 상태: " + ongoingStatus);
		}
		this.ongoingStatus = QuestionSetOngoingStatus.AFTER;
		this.endTime = LocalDateTime.now();
	}

	private void checkLiveDeliveryMode() {
		if (deliveryMode != DeliveryMode.LIVE_TIME) {
			throw new QuestionSetLiveException("LIVE_TIME 모드가 아닌 문제셋은 실시간 시작할 수 없습니다. 현재 모드: " + deliveryMode);
		}
	}

	public void completeQuestionSet(String title, String subject, DeliveryMode mode, String difficulty,
		QuestionSetVisibility visibility) {
		this.title = title;
		this.subject = subject;
		this.deliveryMode = mode;
		this.difficulty = difficulty;
		this.visibility = visibility;
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateMode(DeliveryMode deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	public void updateVisibility(QuestionSetVisibility visibility) {
		this.visibility = visibility;
	}

	public boolean canReview() {
		return ongoingStatus == QuestionSetOngoingStatus.AFTER && deliveryMode == DeliveryMode.REVIEW;
	}

	public void updateOngoingStatus(QuestionSetOngoingStatus status) {
		this.ongoingStatus = status;
	}
}
