package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetMaterialEntity;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetMaterialEntityRepository;
import com.coniv.mait.domain.question.service.dto.QuestionSetMaterialDto;
import com.coniv.mait.global.component.FileUploader;
import com.coniv.mait.global.component.dto.FileInfo;
import com.coniv.mait.global.enums.FileExtension;
import com.coniv.mait.global.s3.dto.FileType;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionSetMaterialService 단위 테스트")
class QuestionSetMaterialServiceTest {

	@InjectMocks
	private QuestionSetMaterialService questionSetMaterialService;

	@Mock
	private QuestionSetMaterialEntityRepository questionSetMaterialEntityRepository;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private FileUploader fileUploader;

	@Mock
	private MultipartFile mockFile;

	@Test
	@DisplayName("문제 세트 자료 업로드 성공")
	void uploadQuestionSetMaterial_Success() {
		// given
		Long questionSetId = 1L;
		Long materialId = 100L;
		String fileUrl = "https://s3.amazonaws.com/bucket/test-file.pdf";
		String fileKey = "question-set-materials/test-file.pdf";
		String bucketName = "mait-bucket";

		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.id(questionSetId)
			.subject("수학")
			.title("중간고사 문제")
			.build();

		FileInfo fileInfo = FileInfo.builder()
			.url(fileUrl)
			.key(fileKey)
			.bucket(bucketName)
			.extension(FileExtension.PDF)
			.build();

		QuestionSetMaterialEntity savedMaterial = QuestionSetMaterialEntity.builder()
			.id(materialId)
			.url(fileUrl)
			.fileKey(fileKey)
			.bucket(bucketName)
			.build();

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(fileUploader.uploadFile(mockFile, FileType.QUESTION_SET_MATERIAL)).thenReturn(fileInfo);
		when(questionSetMaterialEntityRepository.save(any(QuestionSetMaterialEntity.class))).thenReturn(savedMaterial);

		// when
		QuestionSetMaterialDto result = questionSetMaterialService.uploadQuestionSetMaterial(mockFile);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(materialId);
		assertThat(result.getMaterialUrl()).isEqualTo(fileUrl);
		assertThat(result.getMaterialKey()).isEqualTo(fileKey);

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(fileUploader).uploadFile(mockFile, FileType.QUESTION_SET_MATERIAL);
		verify(questionSetMaterialEntityRepository).save(any(QuestionSetMaterialEntity.class));
	}

	@Test
	@DisplayName("존재하지 않는 문제 세트에 자료 업로드 시 EntityNotFoundException 발생")
	void uploadQuestionSetMaterial_QuestionSetNotFound() {
		// given
		Long nonExistentQuestionSetId = 999L;

		when(questionSetEntityRepository.findById(nonExistentQuestionSetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionSetMaterialService.uploadQuestionSetMaterial(mockFile)).isInstanceOf(
				EntityNotFoundException.class)
			.hasMessageContaining("Question Set not found with id: " + nonExistentQuestionSetId);

		verify(questionSetEntityRepository).findById(nonExistentQuestionSetId);
	}

	@Test
	@DisplayName("파일 업로드 후 QuestionSetMaterialEntity가 올바르게 저장됨")
	void uploadQuestionSetMaterial_EntitySavedCorrectly() {
		// given
		Long questionSetId = 1L;
		String fileUrl = "https://s3.amazonaws.com/bucket/test-file.pdf";
		String fileKey = "question-set-materials/test-file.pdf";
		String bucketName = "mait-bucket";

		QuestionSetEntity questionSet = QuestionSetEntity.builder().id(questionSetId).subject("과학").build();

		FileInfo fileInfo = FileInfo.builder()
			.url(fileUrl)
			.key(fileKey)
			.bucket(bucketName)
			.extension(FileExtension.PDF)
			.build();

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(fileUploader.uploadFile(mockFile, FileType.QUESTION_SET_MATERIAL)).thenReturn(fileInfo);
		when(questionSetMaterialEntityRepository.save(any(QuestionSetMaterialEntity.class))).thenAnswer(invocation -> {
			QuestionSetMaterialEntity entity = invocation.getArgument(0);
			return QuestionSetMaterialEntity.builder()
				.id(1L)
				.url(entity.getUrl())
				.fileKey(entity.getFileKey())
				.bucket(entity.getBucket())
				.build();
		});

		// when
		questionSetMaterialService.uploadQuestionSetMaterial(mockFile);

		// then
		verify(questionSetMaterialEntityRepository).save(any(QuestionSetMaterialEntity.class));
	}
}

