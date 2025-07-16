package com.coniv.mait.domain.question.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSetDto {
	private Long id;
	private String subject;
	private String title;
	private String creationType;
	private String visibility;
	private String deliveryMode;
}
