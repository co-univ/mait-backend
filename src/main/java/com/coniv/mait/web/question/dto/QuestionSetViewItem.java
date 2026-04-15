package com.coniv.mait.web.question.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.enums.UserStudyStatus;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "문제 셋 목록 조회 항목")
public record QuestionSetViewItem(
	@Schema(description = "문제 셋 ID")
	Long id,
	@Schema(description = "문제 셋 제목")
	String title,
	@Schema(description = "과목")
	String subject,
	@Schema(description = "문제 셋 생성 유형")
	QuestionSetCreationType creationType,
	@Schema(description = "문제 셋 노출 단위")
	QuestionSetVisibility visibility,
	@Schema(description = "문제 셋 풀이 방식")
	QuestionSetSolveMode solveMode,
	@Schema(description = "문제 셋 상태")
	QuestionSetStatus status,
	@Schema(description = "팀 ID")
	Long teamId,
	@Schema(description = "난이도")
	String difficulty,
	@Schema(description = "유저 학습 상태")
	UserStudyStatus userStudyStatus,
	@Schema(description = "학습 세션 ID")
	Long solvingSessionId,
	@Schema(description = "유저 실시간 참여 상태")
	UserParticipationStatus userParticipationStatus,
	@Schema(description = "실시간 참여 ID")
	Long participantId,
	@Schema(description = "실시간 참가 여부")
	Boolean participated,
	@Schema(description = "실시간 참가자 상태")
	ParticipantStatus participantStatus,
	@Schema(description = "최종 수정 일시")
	LocalDateTime updatedAt
) {

	public static QuestionSetViewItem from(final QuestionSetDto questionSet) {
		return QuestionSetViewItem.builder()
			.id(questionSet.getId())
			.title(questionSet.getTitle())
			.subject(questionSet.getSubject())
			.creationType(questionSet.getCreationType())
			.visibility(questionSet.getVisibility())
			.solveMode(questionSet.getSolveMode())
			.status(questionSet.getStatus())
			.teamId(questionSet.getTeamId())
			.difficulty(questionSet.getDifficulty())
			.updatedAt(questionSet.getUpdatedAt())
			.build();
	}

	public static QuestionSetViewItem from(final StudyQuestionSetDto questionSet) {
		return QuestionSetViewItem.builder()
			.id(questionSet.getId())
			.title(questionSet.getTitle())
			.subject(questionSet.getSubject())
			.status(questionSet.getStatus())
			.difficulty(questionSet.getDifficulty())
			.userStudyStatus(questionSet.getUserStudyStatus())
			.solvingSessionId(questionSet.getSolvingSessionId())
			.updatedAt(questionSet.getUpdatedAt())
			.build();
	}

	public static QuestionSetViewItem of(final QuestionSetEntity questionSet,
		final QuestionSetParticipantEntity participant, final UserParticipationStatus userParticipationStatus) {
		return QuestionSetViewItem.builder()
			.id(questionSet.getId())
			.title(questionSet.getTitle())
			.subject(questionSet.getSubject())
			.creationType(questionSet.getCreationType())
			.visibility(questionSet.getVisibility())
			.solveMode(questionSet.getSolveMode())
			.status(questionSet.getStatus())
			.teamId(questionSet.getTeamId())
			.difficulty(questionSet.getDifficulty())
			.userParticipationStatus(userParticipationStatus)
			.participantId(participant == null ? null : participant.getId())
			.participated(participant != null)
			.participantStatus(participant == null ? null : participant.getStatus())
			.updatedAt(questionSet.getModifiedAt())
			.build();
	}
}
