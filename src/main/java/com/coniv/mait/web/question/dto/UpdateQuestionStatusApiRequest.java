package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.QuestionStatusType;

public record UpdateQuestionStatusApiRequest(
	QuestionStatusType statusType
) {
}
