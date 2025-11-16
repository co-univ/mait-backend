package com.coniv.mait.domain.question.entity;

import jakarta.persistence.Entity;
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

@Table(name = "multiple_choices")
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MultipleChoiceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private int number;

	private String content;

	private boolean isCorrect;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_id", nullable = false)
	private MultipleQuestionEntity question;

	public static MultipleChoiceEntity defaultChoice(int number, MultipleQuestionEntity defaultQuestion) {
		return MultipleChoiceEntity.builder()
			.number(number)
			.isCorrect(false)
			.question(defaultQuestion)
			.build();
	}

	public void updateIsCorrect(boolean isCorrect) {
		this.isCorrect = isCorrect;
	}
}
