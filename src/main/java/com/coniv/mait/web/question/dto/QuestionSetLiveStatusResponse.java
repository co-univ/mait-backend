package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.QuestionSetLiveStatus;

public record QuestionSetLiveStatusResponse(
	Long questionSetId,
	QuestionSetLiveStatus liveStatus
) {

	public static QuestionSetLiveStatusResponse from(Long questionSetId, QuestionSetLiveStatus liveStatus) {
		return new QuestionSetLiveStatusResponse(questionSetId, liveStatus);
	}
}
