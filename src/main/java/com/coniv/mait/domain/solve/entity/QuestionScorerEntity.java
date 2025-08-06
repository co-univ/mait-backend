package com.coniv.mait.domain.solve.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "question_scorers")
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionScorerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private Long questionId;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long submitOrder;

	public void updateScorer(Long userId, Long submitOrder) {
		this.userId = userId;
		this.submitOrder = submitOrder;
	}
}
