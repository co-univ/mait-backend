package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.QuestionSetStatus;

public record QuestionSetLiveStatusResponse(
	Long questionSetId,
	QuestionSetStatus liveStatus
) {

	public static QuestionSetLiveStatusResponse from(Long questionSetId, QuestionSetStatus liveStatus) {
		return new QuestionSetLiveStatusResponse(questionSetId, liveStatus);
	}
}
