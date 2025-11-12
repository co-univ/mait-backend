package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.entity.QuestionSetMaterialEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSetMaterialDto {

	private Long id;

	private String materialUrl;

	private String materialKey;

	public static QuestionSetMaterialDto from(QuestionSetMaterialEntity questionSetMaterial) {
		return QuestionSetMaterialDto.builder()
			.id(questionSetMaterial.getId())
			.materialUrl(questionSetMaterial.getUrl())
			.materialKey(questionSetMaterial.getFileKey())
			.build();
	}
}
