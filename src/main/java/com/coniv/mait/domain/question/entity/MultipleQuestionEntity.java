package com.coniv.mait.domain.question.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("mutliple")
public class MultipleQuestionEntity extends QuestionEntity {
}
