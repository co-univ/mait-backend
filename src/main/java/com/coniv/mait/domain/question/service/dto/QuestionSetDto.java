package com.coniv.mait.domain.question.service.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.coniv.mait.domain.question.dto.MaterialDto;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSetDto {
	private Long id;
	private String title;
	private QuestionSetCreationType creationType;
	private QuestionSetVisibility visibility;
	private QuestionSetSolveMode solveMode;
	private QuestionSetStatus status;
	private Long teamId;
	private Long questionCount;
	private String difficulty;
	private String instruction;
	private List<MaterialDto> materials;
	private List<QuestionSetCategoryDto> categories;
	private List<QuestionTypeCount> questionTypeCounts;
	private LocalDateTime updatedAt;

	public static QuestionSetDto from(final QuestionSetEntity questionSetEntity) {
		return QuestionSetDto.builder()
			.id(questionSetEntity.getId())
			.title(questionSetEntity.getTitle())
			.creationType(questionSetEntity.getCreationType())
			.visibility(questionSetEntity.getVisibility())
			.solveMode(questionSetEntity.getSolveMode())
			.status(questionSetEntity.getStatus())
			.teamId(questionSetEntity.getTeamId())
			.difficulty(questionSetEntity.getDifficulty())
			.instruction(questionSetEntity.getInstruction())
			.updatedAt(questionSetEntity.getModifiedAt())
			.categories(List.of())
			.build();
	}

	public static QuestionSetDto of(QuestionSetEntity questionSetEntity, long questionCount,
		List<QuestionSetCategoryDto> categories, List<QuestionTypeCount> questionTypeCounts) {
		return QuestionSetDto.builder()
			.id(questionSetEntity.getId())
			.title(questionSetEntity.getTitle())
			.creationType(questionSetEntity.getCreationType())
			.visibility(questionSetEntity.getVisibility())
			.solveMode(questionSetEntity.getSolveMode())
			.teamId(questionSetEntity.getTeamId())
			.difficulty(questionSetEntity.getDifficulty())
			.instruction(questionSetEntity.getInstruction())
			.status(questionSetEntity.getStatus())
			.updatedAt(questionSetEntity.getModifiedAt())
			.questionCount(questionCount)
			.categories(categories)
			.questionTypeCounts(questionTypeCounts)
			.build();
	}

	@Deprecated
	@JsonProperty("subject")
	@Schema(description = "문제 셋 제목(deprecated, title과 동일)", deprecated = true)
	public String getSubject() {
		return title;
	}
}
