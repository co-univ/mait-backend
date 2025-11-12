package com.coniv.mait.domain.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
}
