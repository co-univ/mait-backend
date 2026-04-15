package com.coniv.mait.web.question.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.UserStudyStatus;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "학습 모드 풀이 - 문제 셋")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyQuestionSetDto {
	@Schema(description = "문제 셋 ID")
	private Long id;
	@Schema(description = "문제 셋 제목")
	private String title;
	@Schema(description = "과목")
	private String subject;
	@Schema(description = "전역 진행 상태")
	private QuestionSetStatus status;
	@Schema(description = "난이도")
	private String difficulty;
	@Schema(description = "유저 풀이 상태")
	private UserStudyStatus userStudyStatus;
	@Schema(description = "풀이 세션 ID (풀이 전인 경우 null)")
	private Long solvingSessionId;
	@Schema(description = "최종 수정 일시")
	private LocalDateTime updatedAt;

	public static StudyQuestionSetDto of(final QuestionSetEntity questionSet,
		final SolvingSessionEntity solvingSession) {
		Long solvingSessionId = solvingSession != null ? solvingSession.getId() : null;

		return StudyQuestionSetDto.builder()
			.id(questionSet.getId())
			.title(questionSet.getTitle())
			.subject(questionSet.getSubject())
			.status(questionSet.getStatus())
			.difficulty(questionSet.getDifficulty())
			.userStudyStatus(UserStudyStatus.from(solvingSession))
			.solvingSessionId(solvingSessionId)
			.updatedAt(questionSet.getModifiedAt()).build();
	}
}
