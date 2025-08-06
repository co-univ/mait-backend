package com.coniv.mait.domain.question.entity;

import com.coniv.mait.domain.question.enums.ParticipantStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "question_set_participants",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "question_set_participants_uk",
			columnNames = {"question_set_id", "user_id"}
		)
	}
)
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionSetParticipantEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_set_id", nullable = false)
	private QuestionSetEntity questionSet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private ParticipantStatus status = ParticipantStatus.ACTIVE;

	public static QuestionSetParticipantEntity createActiveParticipant(QuestionSetEntity questionSet, UserEntity user) {
		return QuestionSetParticipantEntity.builder()
			.questionSet(questionSet)
			.user(user)
			.status(ParticipantStatus.ACTIVE)
			.build();
	}

	public String getParticipantName() {
		return user.getName();
	}
}
