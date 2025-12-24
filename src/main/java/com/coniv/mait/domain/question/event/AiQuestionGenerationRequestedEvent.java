package com.coniv.mait.domain.question.event;

import java.util.List;

import com.coniv.mait.domain.question.dto.MaterialDto;
import com.coniv.mait.domain.question.service.dto.QuestionCount;

public record AiQuestionGenerationRequestedEvent(
	Long questionSetId,
	List<QuestionCount> counts,
	List<MaterialDto> materials,
	String instruction,
	String difficulty
) {
}


