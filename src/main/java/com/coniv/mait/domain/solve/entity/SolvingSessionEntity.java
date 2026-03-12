package com.coniv.mait.domain.solve.entity;

import java.time.LocalDateTime;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "solving_sessions")
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolvingSessionEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_set_id", nullable = false)
	private QuestionSetEntity questionSet;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private SolvingStatus status = SolvingStatus.PROGRESSING;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private DeliveryMode mode = DeliveryMode.STUDY;

	private LocalDateTime startedAt;

	private LocalDateTime submittedAt;

	private Integer totalCount;

	private Integer correctCount;

	public void submit(int totalCount, int correctCount) {
		this.status = SolvingStatus.COMPLETE;
		this.submittedAt = LocalDateTime.now();
		this.totalCount = totalCount;
		this.correctCount = correctCount;
	}

	public static SolvingSessionEntity studySession(UserEntity user, QuestionSetEntity questionSet) {
		return SolvingSessionEntity.builder()
			.user(user)
			.questionSet(questionSet)
			.mode(DeliveryMode.STUDY)
			.status(SolvingStatus.PROGRESSING)
			.startedAt(LocalDateTime.now())
			.build();
	}
}
