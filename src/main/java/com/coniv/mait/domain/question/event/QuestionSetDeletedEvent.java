package com.coniv.mait.domain.question.event;

import java.util.List;

import com.coniv.mait.global.event.MaitEvent;

public record QuestionSetDeletedEvent(
	Long questionSetId,
	List<Long> questionIds,
	List<Long> imageIds
) implements MaitEvent {
}
