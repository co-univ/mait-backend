package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MultipleQuestionDto {

	private Long id;

	private String content;

	private String explanation;

	private Long number;

	private List<MultipleChoiceDto> choices;
}
