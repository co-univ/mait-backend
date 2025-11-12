package com.coniv.mait.domain.question.external.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.QuestionDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiCreateResponse {
	private List<QuestionDto> content;
}
