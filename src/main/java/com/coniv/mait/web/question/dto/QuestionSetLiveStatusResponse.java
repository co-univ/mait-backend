package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;

public record QuestionSetLiveStatusResponse(
	Long questionSetId,
	QuestionSetOngoingStatus liveStatus
) {

	public static QuestionSetLiveStatusResponse from(Long questionSetId, QuestionSetOngoingStatus liveStatus) {
		return new QuestionSetLiveStatusResponse(questionSetId, liveStatus);
	}
}
