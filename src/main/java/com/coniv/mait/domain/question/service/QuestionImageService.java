package com.coniv.mait.domain.question.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionImageEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionImageEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionImageDto;
import com.coniv.mait.global.component.ImageUploader;
import com.coniv.mait.global.component.dto.ImageInfo;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionImageService {

	private static final String QUESTION_IMAGE_DIRECTORY = "questions";

	private final QuestionEntityRepository questionEntityRepository;
	private final ImageUploader imageUploader;
	private final QuestionImageEntityRepository questionImageEntityRepository;

	@Transactional
	public QuestionImageDto uploadImage(final Long questionId, final MultipartFile image) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));

		ImageInfo imageInfo = imageUploader.uploadImage(image, QUESTION_IMAGE_DIRECTORY);

		QuestionImageEntity questionImage = questionImageEntityRepository.save(QuestionImageEntity.builder()
			.question(question)
			.imageKey(imageInfo.getKey())
			.url(imageInfo.getUrl())
			.bucket(imageInfo.getBucket())
			.build());

		return QuestionImageDto.builder()
			.id(questionImage.getId())
			.questionId(question.getId())
			.imageKey(questionImage.getImageKey())
			.imageUrl(questionImage.getUrl())
			.build();
	}

	@Async("maitThreadPoolExecutor")
	@Transactional
	public void updateImage(final QuestionEntity question, final Long questionImageId) {
		if (questionImageId == null) {
			return;
		}

		questionImageEntityRepository.findAllByQuestionAndUsedIsTrue(question).forEach(image -> {
			image.updateUsage(false);
		});

		QuestionImageEntity questionImage = questionImageEntityRepository.findById(questionImageId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionImage not found with id: " + questionImageId));

		questionImage.updateUsage(true);
		questionImageEntityRepository.save(questionImage);
	}
}
