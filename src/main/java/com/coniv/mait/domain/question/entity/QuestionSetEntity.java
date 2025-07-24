package com.coniv.mait.domain.question.entity;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "question_sets")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionSetEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String subject;

	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QuestionSetCreationType creationType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QuestionSetVisibility visibility = QuestionSetVisibility.GROUP;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DeliveryMode deliveryMode = DeliveryMode.LIVE_TIME;

	private QuestionSetEntity(String subject, QuestionSetCreationType creationType) {
		this.subject = subject;
		this.creationType = creationType;
	}

	public static QuestionSetEntity of(String subject, QuestionSetCreationType creationType) {
		return new QuestionSetEntity(subject, creationType);
	}
}
