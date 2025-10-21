package com.coniv.mait.domain.question.entity;

import com.coniv.mait.domain.question.constant.QuestionConstant;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Table(name = "questions")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "question_type")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class QuestionEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(columnDefinition = "TEXT")
	private String content;

	private String explanation;

	private Long number;

	@Column(nullable = false)
	private String lexoRank;

	private String imageUrl;

	@Column(nullable = false)
	private int displayDelayMilliseconds;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private QuestionStatusType questionStatus = QuestionStatusType.NOT_OPEN;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_set_id")
	private QuestionSetEntity questionSet;

	public static QuestionEntity createDefaultQuestion(QuestionSetEntity questionSet, String rank) {
		return MultipleQuestionEntity.builder()
			.questionSet(questionSet)
			.lexoRank(rank)
			.content(QuestionConstant.DEFAULT_QUESTION_CONTENT)
			.displayDelayMilliseconds(QuestionConstant.MAX_DISPLAY_DELAY_MILLISECONDS)
			.build();
	}

	public void updateQuestionStatus(QuestionStatusType questionStatus) {
		this.questionStatus = questionStatus;
	}

	public QuestionType getType() {
		switch (this) {
			case MultipleQuestionEntity multipleQuestion -> {
				return QuestionType.MULTIPLE;
			}
			case ShortQuestionEntity shortQuestionEntity -> {
				return QuestionType.SHORT;
			}
			case FillBlankQuestionEntity fillBlankQuestionEntity -> {
				return QuestionType.FILL_BLANK;
			}
			case OrderingQuestionEntity orderingQuestionEntity -> {
				return QuestionType.ORDERING;
			}
			default -> throw new IllegalArgumentException("지원하지 않는 문제 유형입니다: " + this.getClass().getSimpleName());
		}
	}

	public void updateContent(final String content) {
		this.content = content;
	}

	public void updateExplanation(final String explanation) {
		this.explanation = explanation;
	}

	public void updateNumber(long number) {
		this.number = number;
	}

	public void updateRank(String rank) {
		this.lexoRank = rank;
	}

	public void updateImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}
