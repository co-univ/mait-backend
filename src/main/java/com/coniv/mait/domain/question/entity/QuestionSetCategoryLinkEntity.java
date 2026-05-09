package com.coniv.mait.domain.question.entity;

import com.coniv.mait.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
	name = "question_set_category_links",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_question_set_category_links_set_category",
			columnNames = {"question_set_id", "category_id"})
	},
	indexes = {
		@Index(name = "idx_question_set_category_links_category_set",
			columnList = "category_id, question_set_id")
	}
)
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionSetCategoryLinkEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_set_id", nullable = false)
	private Long questionSetId;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	private QuestionSetCategoryLinkEntity(final Long questionSetId, final Long categoryId) {
		this.questionSetId = questionSetId;
		this.categoryId = categoryId;
	}

	public static QuestionSetCategoryLinkEntity of(final Long questionSetId, final Long categoryId) {
		return new QuestionSetCategoryLinkEntity(questionSetId, categoryId);
	}
}
