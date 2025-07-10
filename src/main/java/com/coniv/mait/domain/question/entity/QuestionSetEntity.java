package com.coniv.mait.domain.question.entity;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "question_sets")
@Entity
@Getter
@NoArgsConstructor
public class QuestionSetEntity {

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
}
