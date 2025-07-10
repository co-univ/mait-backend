package com.coniv.mait.domain.question.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("fill_blank")
public class FillBlankQuestionEntity extends QuestionEntity {
}
