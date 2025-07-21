package com.coniv.mait.domain.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Table(name = "questions")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "question_type")
public abstract class QuestionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String content;

	private String explanation;

	@Column(nullable = false)
	private Long number;

	@Column(nullable = false)
	private int displayDelayMilliseconds;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_set_id")
	private QuestionSetEntity questionSet;
}
