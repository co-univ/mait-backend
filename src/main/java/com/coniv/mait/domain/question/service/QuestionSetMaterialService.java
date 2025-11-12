package com.coniv.mait.domain.question.service;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.coniv.mait.domain.question.dto.MaterialDto;
import com.coniv.mait.domain.question.entity.QuestionSetMaterialEntity;
import com.coniv.mait.domain.question.repository.QuestionSetMaterialEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetMaterialDto;
import com.coniv.mait.global.component.FileUploader;
import com.coniv.mait.global.component.dto.FileInfo;
import com.coniv.mait.global.s3.dto.FileType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionSetMaterialService {

	private final QuestionSetMaterialEntityRepository questionSetMaterialEntityRepository;

	private final FileUploader fileUploader;

	@Transactional
	public QuestionSetMaterialDto uploadQuestionSetMaterial(final MultipartFile material) {
		FileInfo fileInfo = fileUploader.uploadFile(material, FileType.QUESTION_SET_MATERIAL);

		QuestionSetMaterialEntity questionSetMaterial = questionSetMaterialEntityRepository.save(
			QuestionSetMaterialEntity.builder()
				.fileKey(fileInfo.getKey())
				.url(fileInfo.getUrl())
				.bucket(fileInfo.getBucket())
				.used(false)
				.build()
		);

		return QuestionSetMaterialDto.from(questionSetMaterial);
	}

	@Async("maitThreadPoolExecutor")
	@Transactional
	public void updateUsed(List<MaterialDto> materials) {
		for (MaterialDto material : materials) {
			QuestionSetMaterialEntity entity = questionSetMaterialEntityRepository.findById(material.getId())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제 셋 자료입니다. id=" + material.getId()));
			entity.updateUsage(true);
		}
	}
}
