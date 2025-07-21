package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BaseUpdateQuestionApiRequest {

	private List<MultipleQuestionDto> multipleQuestions;
}
