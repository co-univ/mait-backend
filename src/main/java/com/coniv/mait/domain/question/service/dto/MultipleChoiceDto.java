package com.coniv.mait.domain.question.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MultipleChoiceDto {

	private Long id;

	private int number;

	private String content;

	private boolean isCorrect;
}
