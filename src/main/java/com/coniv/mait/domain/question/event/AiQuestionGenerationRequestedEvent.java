package com.coniv.mait.domain.question.event;

import java.util.List;

import com.coniv.mait.domain.question.dto.MaterialDto;
import com.coniv.mait.domain.question.service.dto.QuestionCount;
import com.coniv.mait.global.event.MaitEvent;

import lombok.Builder;

@Builder
public record AiQuestionGenerationRequestedEvent(
	Long questionSetId,
	List<QuestionCount> counts,
	List<MaterialDto> materials,
	String instruction,
	String difficulty) implements MaitEvent {
}
