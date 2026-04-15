package com.coniv.mait.web.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문제 셋 목록 그룹화 기준")
public enum QuestionSetGroupBy {
	NONE,
	QUESTION_SET_STATUS,
	USER_PARTICIPATION_STATUS,
	USER_STUDY_STATUS
}
