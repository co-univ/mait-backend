package com.coniv.mait.domain.question.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.coniv.mait.domain.question.entity.QuestionImageEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionImageEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionImageDto;
import com.coniv.mait.global.component.dto.FileInfo;
import com.coniv.mait.global.s3.dto.FileType;
import com.coniv.mait.global.s3.service.S3FileUploader;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionImageService {

	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final S3FileUploader imageUploader;
	private final QuestionImageEntityRepository questionImageEntityRepository;

	@Transactional
	public QuestionImageDto uploadImage(final Long questionSetId, final MultipartFile image) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("Question Set not found with id: " + questionSetId));

		FileInfo imageInfo = imageUploader.uploadFile(image, FileType.QUESTION_IMAGE);

		QuestionImageEntity questionImage = questionImageEntityRepository.save(QuestionImageEntity.builder()
			.fileKey(imageInfo.getKey())
			.url(imageInfo.getUrl())
			.bucket(imageInfo.getBucket())
			.build());

		return QuestionImageDto.builder()
			.id(questionImage.getId())
			.imageKey(questionImage.getFileKey())
			.imageUrl(questionImage.getUrl())
			.build();
	}

	@Async("maitThreadPoolExecutor")
	@Transactional
	public CompletableFuture<Void> unuseExistImage(final Long existQuestionImageId) {
		QuestionImageEntity existImage = questionImageEntityRepository.findById(existQuestionImageId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionImage not found id: " + existQuestionImageId));

		existImage.updateUsage(false);
		return CompletableFuture.completedFuture(null);
	}
}
