package com.coniv.mait.domain.question.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionImageDto {

	private Long id;

	private Long questionId;

	private String imageUrl;

	private String imageKey;
}
