package com.coniv.mait.web.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문제 셋 목록 조회 카테고리")
public enum QuestionSetViewType {
	LIVE_TIME,
	STUDY,
	REVIEW,
	MAKING
}
