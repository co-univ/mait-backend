package com.coniv.mait.domain.question.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("multiple")
public class MultipleQuestionEntity extends QuestionEntity {

	private int answerCount;
}
