package com.coniv.mait.domain.question.service;

import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionImageEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionImageEntityRepository;
import com.coniv.mait.global.s3.service.S3FileUploader;

@ExtendWith(MockitoExtension.class)
class QuestionImageServiceTest {

	@InjectMocks
	private QuestionImageService questionImageService;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private S3FileUploader imageUploader;

	@Mock
	private QuestionImageEntityRepository questionImageEntityRepository;

	@Test
	@DisplayName("문제 사진 업데이트 - 성공")
	void unUseImage_Success() throws ExecutionException, InterruptedException {
		// Given
		final Long questionImageId = 1L;

		QuestionImageEntity existingImage = mock(QuestionImageEntity.class);
		when(questionImageEntityRepository.findById(questionImageId)).thenReturn(Optional.of(existingImage));

		// When
		CompletableFuture<Void> future = questionImageService.unUseExistImage(questionImageId);
		future.get();

		// Then
		verify(existingImage).updateUsage(false);
	}
}
