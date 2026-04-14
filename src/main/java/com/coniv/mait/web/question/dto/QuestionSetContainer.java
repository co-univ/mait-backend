package com.coniv.mait.web.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문제 셋 컨테이너 (QuestionSetList 또는 QuestionSetGroup 또는 StudyQuestionSetGroup)",
	oneOf = {QuestionSetList.class, QuestionSetGroup.class, StudyQuestionSetGroup.class})
public sealed interface QuestionSetContainer permits QuestionSetGroup, QuestionSetList, StudyQuestionSetGroup {
}
